package com.topcoder.nasa.job.http;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.topcoder.nasa.job.http.ImageHttpDownloader.ImageHttpDownloaderCallback;

/**
 * .
 * 
 * @author schmoel, TCSDEVELOPER
 * @version 1.0
 *
 */
@Component
public class ImageFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(ImageFetcher.class);

    /**
     * Every time {@link #fetch(String)} is called, it indicates that we have one more image to
     * fetch. Whenever an image finishes downloading (successfully or unsuccessfully), <b>or</b> we
     * recognize that the image as already been downloaded, we have one fewer image to download.
     * <p/>
     * So, this is increased when {@link #fetch(String)} is called and decreased whenever a
     * "done with this image URL" event, as described above, occurs.
     */
    private AtomicInteger imageCountLeftToFetch = new AtomicInteger();

    /**
     * Queue of images left for us to download.
     * <p/>
     * Note that the size of this may not equal the value of {@link #imageCountLeftToFetch}. This is
     * because this queue is polled by {@link #nudgeDownloader()} and the
     * {@link #imageCountLeftToFetch} is only touched after {@link #nudgeDownloader()} has done its
     * thing.
     * <p/>
     * There may be a siutation in which {@link #nudgeDownloader()} has to put a URL back in the
     * queue for processing later - e.g. if the site is down or if the connections to the host have
     * been exceeded.
     */
    private Queue<String> imageUrlsToFetchQueue = new LinkedList<String>();

    /**
     * This class fetches the image from the source - i.e. over HTTP
     */
    @Autowired
    private ImageHttpDownloader httpDownloader;

    // =========================================================================

    /**
     * Called by clients who wish to begin asynchrously fetching the resources (images) at the
     * provided urls.
     * 
     * @param allUrls
     *            the urls to download
     * @return the target locations on disk, where the images will be downloaded to.
     */
    public List<File> fetchAll(List<String> allUrls) {
        List<File> files = new ArrayList<File>();

        for (String url : allUrls) {
            files.add(fetch(url));
        }

        return files;
    }

    /**
     * Called by clients who wish to begin asynchrously fetching the resource at this url.
     * 
     * @param url
     *            the url to download
     * @return the target file on disk, where the image will be downloaded to
     */
    public File fetch(String url) {
        addImageUrlToFetch(url);
        imageCountLeftToFetch.incrementAndGet();

        nudgeDownloader();

        return httpDownloader.getCachedFile(url).getAbsoluteFile();
    }

    /**
     * @return true if we are busy downloading/fetching an image; false if we are idle.
     */
    public boolean isFetching() {
        return imageCountLeftToFetch.get() != 0;
    }

    // =========================================================================

    private synchronized void addImageUrlToFetch(String url) {
        imageUrlsToFetchQueue.add(url);
    }

    private synchronized void addImageUrlsToFetch(List<String> urls) {
        imageUrlsToFetchQueue.addAll(urls);
    }

    private synchronized String pollImageUrlToFetch() {
        return imageUrlsToFetchQueue.poll();
    }

    // =========================================================================

    /**
     * The smarts.
     */
    private void nudgeDownloader() {
        String url = null;

        // if the connection is over used (i.e. we have for example 4 images downloading and are
        // about to start another, and the connection is throttled to a max of 4 connections), we
        // need to add the urls we have taken away back to the job for consideration later.
        List<String> urlsToAddBack = new ArrayList<String>();

        // pull the next image...
        while ((url = pollImageUrlToFetch()) != null) {
            // ...see if it's already in the cache
            File cachedFile = httpDownloader.getCachedFile(url);

            if (cachedFile.exists()) {
                // it is - move on - already downloaded!
                imageCountLeftToFetch.decrementAndGet();
                continue;
            }

            // ...it is not already downloaded, let's try and start fetching it into the cache
            boolean startedFetching = httpDownloader.startFetch(url,
                    new ImageHttpDownloaderCallback() {
                        public void onImageFetchSuccess(String url, File cacheFile) {
                            afterImageFetchCallback();
                        }

                        @Override
                        public void onImageFetchFail(String url) {
                            LOG.error("IMAGE FAILED TO DOWNLOAD {}", url);
                            afterImageFetchCallback();
                        }

                    });

            // if we could not start fetching this image right now (e.g. client too busy)...
            if (!startedFetching) {
                // ... add it back for consideration later.
                urlsToAddBack.add(url);
            }
        }

        // add back all those images we couldn't start fetching for this time, for retry later on.
        addImageUrlsToFetch(urlsToAddBack);
    }

    private void afterImageFetchCallback() {
        imageCountLeftToFetch.decrementAndGet();
        nudgeDownloader();
    }

    // =========================================================================
}
