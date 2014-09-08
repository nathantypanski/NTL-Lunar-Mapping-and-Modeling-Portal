package com.topcoder.nasa.job.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

/**
 * Responsible for fetching images from a URL and depositing them into {@link #downloadDirectory}.
 * <p/>
 * <b>Note that this implementation is pretty simple does NOT consider concurrent requests.</b>
 * Multiple requests to the same URL results in undefined (typically bad) behavior.
 *
 */
public class ImageHttpDownloader {
    private static final Logger LOG = LoggerFactory.getLogger(ImageHttpDownloader.class);

    private static final int MAX_HTTP_CONNECTIONS_PER_HOST = 4;
    private static final int FIVE_MINS_IN_MS = 1000 * 60 * 5;

    // =========================================================================

    /**
     * Provided by clients when they call {@link #startFetch(String, ImageHttpDownloaderCallback)}
     * so that we can inform them of progress asyncrhonously.
     *
     */
    public interface ImageHttpDownloaderCallback {
        /**
         * Oh no! Called when we could not fetch this image. At some point we'll want to include a
         * reason why, but unnecessary for now.
         * 
         * @param url
         *            the URL that failed to download
         */
        void onImageFetchFail(String url);

        /**
         * Called when image was fetched successfully and brought into cache directory.
         * 
         * @param url
         *            the URL that downloaded
         * @param cacheFile
         *            the file it downloaded to
         */
        void onImageFetchSuccess(String url, File cacheFile);
    }

    // =========================================================================

    private AsyncHttpClient httpClient;
    private File downloadDirectory;

    // =========================================================================

    public ImageHttpDownloader() {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder() //
                .setIdleConnectionTimeoutInMs(FIVE_MINS_IN_MS) //
                .setRequestTimeoutInMs(FIVE_MINS_IN_MS) //

                // this is ignored for some reason:
                // http://stackoverflow.com/questions/18260942/nings-asynchttpclient-doesnt-handle-setmaximumconnectionsperhost-correctly
                .setMaximumConnectionsPerHost(MAX_HTTP_CONNECTIONS_PER_HOST) //

                // set a hard limit of 4
                .setMaximumConnectionsTotal(MAX_HTTP_CONNECTIONS_PER_HOST) //
                .build();

        httpClient = new AsyncHttpClient(config);
    }

    // =========================================================================

    /**
     * Sanity check fields.
     */
    @PostConstruct
    public void init() {
        if (!downloadDirectory.exists() || !downloadDirectory.isDirectory()) {
            throw new IllegalStateException("Cache directory " + downloadDirectory
                    + " does not exist");
        }
    }

    /**
     * Clients will want to know if we have already cached the image at a given URL. This method
     * allows them to do that.
     * 
     * @param imageUrl
     *            the url of the image we want to check if cached
     * @return null if image is not cached already; otherwise, File for the image. <b>WARNING:</b>
     *         this may be a download-in-progress.
     */
    public File getCachedFile(String imageUrl) {
        return computeDownloadFile(imageUrl);
    }

    /**
     * Start the fetch for the given image. Returns immediately.
     * 
     * @param imageUrl
     *            the url to download
     * @param callback
     *            what to notify when we have information related to the download.
     * @return true if we started to fetch or false if the connection is too busy and the client
     *         needs to send the request for submission again later
     */
    public boolean startFetch(String imageUrl, ImageHttpDownloaderCallback callback) {
        try {
            return doStartFetch(imageUrl, callback);
        } catch (IOException e) {
            throw new IllegalStateException("Exception whilst fetching image", e);
        }
    }

    /**
     * See {@link FileHandler} for more async callback information.
     */
    private boolean doStartFetch(final String imageUrl, final ImageHttpDownloaderCallback callback)
            throws IOException {

        try {
            httpClient.prepareGet(imageUrl).execute(new FileHandler(imageUrl, callback));
            LOG.info("Downloading {}", imageUrl);
        } catch (IOException e) {
            // if the client reported that there are too many connections open, return false to
            // inform the caller they need to try again later
            if (thrownCozOfTooManyConnectionsToHost(e)) {
                return false;
            }

            throw e;
        }

        return true;
    }

    // =========================================================================

    /**
     * Given an image URL, isolates the filename by stripping the leading protocol/port/path and the
     * query parameter postfix (if any). This is the name used in the cache directory.
     * 
     * @param imageUrl
     *            the url to get the filename for
     * @return the image file name
     */
    private static String computeFileNameFromUrl(String imageUrl) {
        String filePath = null;

        try {
            filePath = new URL(imageUrl).getPath();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(
                    "Malformed URL encountered while parsing file name from URL", e);
        }

        String fileName = new File(filePath).getName();

        int queryIndex = fileName.indexOf('?');

        if (queryIndex != -1) {
            fileName = fileName.substring(0, queryIndex);
        }

        return fileName;
    }

    private File computeDownloadFile(String imageUrl) {
        return new File(downloadDirectory, computeFileNameFromUrl(imageUrl));
    }

    private boolean thrownCozOfTooManyConnectionsToHost(Throwable t) {
        if (t.getMessage().indexOf("Too many connections") != -1) {
            return true;
        }

        return false;
    }

    // =========================================================================

    public void setDownloadDirectory(File cacheDirectory) {
        this.downloadDirectory = cacheDirectory;
    }

    // =========================================================================

    /**
     * Really simple {@link AsyncHandler} implementation responsible for actually saving the file to
     * disk and for logging simple download progress information.
     */
    private class FileHandler implements AsyncHandler<File> {
        /** The URL being downloaded */
        private String url;

        /** The file we're writing to */
        private File file;

        /** An OutputStream for the file we're writing to */
        private FileOutputStream fos;

        /** What to call with informational updates */
        private ImageHttpDownloaderCallback callback;

        /**
         * The last percentage completed we logged, see
         * {@link #onBodyPartReceived(HttpResponseBodyPart)}
         */
        private float lastPercentage;

        private long totalBytesRead;
        private Long fileSize;

        public FileHandler(String url, ImageHttpDownloaderCallback callback)
                throws FileNotFoundException {
            this.url = url;
            this.callback = callback;

            this.file = computeDownloadFile(url);
        }

        @Override
        public void onThrowable(Throwable t) {
            if (thrownCozOfTooManyConnectionsToHost(t)) {
                return;
            }

            failed();
        }

        @Override
        public com.ning.http.client.AsyncHandler.STATE onBodyPartReceived(
                HttpResponseBodyPart bodyPart) throws Exception {
            int bytesRead = bodyPart.getBodyPartBytes().length;
            totalBytesRead += bytesRead;

            if (fileSize == null) {
                LOG.debug(
                        "Downloaded another {} bytes of url {} for a total of {} bytes read so far",
                        bytesRead, url, totalBytesRead);
            } else {
                float percentage = (float) totalBytesRead / fileSize * 100;

                if (percentage - lastPercentage > 1.0f) {
                    LOG.info("Downloaded {}% of {}", percentage, url);
                    lastPercentage = percentage;
                }

            }

            bodyPart.writeTo(fos);

            return STATE.CONTINUE;
        }

        @Override
        public com.ning.http.client.AsyncHandler.STATE onStatusReceived(
                HttpResponseStatus responseStatus) throws Exception {
            int code = responseStatus.getStatusCode();

            if (code == 200) {
                this.fos = new FileOutputStream(file);
                return STATE.CONTINUE;
            }

            LOG.error("Expected status code 200 but got: " + code);

            failed();
            return STATE.ABORT;
        }

        @Override
        public com.ning.http.client.AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers)
                throws Exception {
            String contentLength = headers.getHeaders().getFirstValue("content-length");

            if (contentLength != null) {
                fileSize = Long.valueOf(contentLength);
            }

            return STATE.CONTINUE;
        }

        @Override
        public File onCompleted() throws Exception {
            fos.close();

            return onCompleted_success();
        }

        private File onCompleted_success() {
            LOG.info("Downloaded {}", url);
            callback.onImageFetchSuccess(url, file);

            return file;
        }

        private File failed() {
            LOG.info("Failed to download {}", url);
            file.delete();

            callback.onImageFetchFail(url);

            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                LOG.error("Couldn't close stream", e);
            }

            return null;
        }
    }
}
