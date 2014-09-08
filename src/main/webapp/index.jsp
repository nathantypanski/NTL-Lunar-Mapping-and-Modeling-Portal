<html>
<head>
<title>NASA LMMP Image Generate REST API</title>
<script type="text/javascript"
	src="//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<script type="text/javascript"
	src="//cdnjs.cloudflare.com/ajax/libs/json2/20130526/json2.min.js"></script>

</head>
<body>
	<h1>NASA LMMP Image Generation REST API</h1>

	<!-- .................................................................................................................. -->
	<!-- .................................................................................................................. -->
	<!-- ........GENERATE NEW MOSAIC....................................................................................... -->
	<h2>Generate a new mosaic</h2>

	<form id="create-form">
		<table>
			<tr>
				<td>Data Set Id:</td>
				<td><input id="data-set-id" /></td>
				<td>(Comma-Separated) Target Types:</td>
				<td><input id="target-types" /></td>
			</tr>
			<tr>
				<td>(Comma-Separated) Targets:</td>
				<td><input id="targets" /></td>
				<td>(Comma-Separated) Missions:</td>
				<td><input id="missions" /></td>
			</tr>
			<tr>
				<td>(Comma-Separated) Instruments:</td>
				<td><input id="instruments" /></td>
				<td>(Comma-Separated) Instrument Hosts:</td>
				<td><input id="instrument-hosts" /></td>
			</tr>
			<tr>
				<td>Start Date:</td>
				<td><input id="start-date" /></td>
				<td>Stop Date:</td>
				<td><input id="stop-date" /></td>
			</tr>
			<tr>
				<td>Product Type:</td>
				<td><input id="product-type" /></td>
				<td>Camera Specification:</td>
				<td><input id="camera-spec" /></td>
			</tr>
			<tr>
				<td>Min Longitude:</td>
				<td><input id="longitude-min" /></td>
				<td>Max Longitude:</td>
				<td><input id="longitude-max" /></td>
			</tr>
			<tr>
				<td>Min Latitude:</td>
				<td><input id="latitude-min" />
				<td>Max Latitude:</td>
				<td><input id="latitude-max" />
			</tr>
			<tr>
				<td>Min Illumination:</td>
				<td><input id="illumination-min" />
				<td>Max Illumination:</td>
				<td><input id="illumination-max" />
			</tr>
			<tr>
				<td>Min Camera Angle:</td>
				<td><input id="camera-angle-min" />
				<td>Max Camera Angle:</td>
				<td><input id="camera-angle-max" />
			</tr>
			<tr>
				<td>File Format:</td>
				<td><select id="file-format">
						<option value="png">PNG</option>
						<option value="gtiff">GeoTIFF</option>
				</select></td>
				<td>&nbsp;</td>
				<td align="right"><input id="generate-button" type="button"
					value="Generate" /></td>
			</tr>
			<tr>
				<td colspan="4"><div id="create-status" /></td>
			</tr>
		</table>
	</form>

	<script type='text/javascript'>
    function pushIfSet(request, key, id) {
      val = $(id).val();

      if (!val) {
        return;
      }

      request[key] = val;
    }

    function pushAsArrayIfSet(request, key, id) {
      val = $(id).val();

      if (!val) {
        return;
      }

      request[key] = $.map(val.split(','), $.trim);
    }

    /* attach a submit handler to the form */
    $("#generate-button").click(
        function(event) {
          /* stop form from submitting normally */
          event.preventDefault();

          var request = {};
          pushIfSet(request, "dataSetId", "#data-set-id");
          pushIfSet(request, "startDate", "#start-date");
          pushIfSet(request, "stopDate", "#stop-date");
          pushIfSet(request, "productType", "#product-type")
          pushIfSet(request, "cameraSpecification", "#camera-specification");
          pushIfSet(request, "longitudeMin", "#longitude-min");
          pushIfSet(request, "longitudeMax", "#longitude-max");
          pushIfSet(request, "latitudeMin", "#latitude-min");
          pushIfSet(request, "latitudeMax", "#latitude-max");
          pushIfSet(request, "illuminationMin", "#illumination-min");
          pushIfSet(request, "illuminationMax", "#illumination-max");
          pushIfSet(request, "cameraAngleMin", "#camera-angle-min");
          pushIfSet(request, "cameraAngleMax", "#camera-angle-max");
          pushIfSet(request, "outputFormat", "#file-format");
          pushAsArrayIfSet(request, "targetTypes", "#target-types");
          pushAsArrayIfSet(request, "targets", "#targets");
          pushAsArrayIfSet(request, "missions", "#missions");
          pushAsArrayIfSet(request, "instruments", "#instruments");
          pushAsArrayIfSet(request, "instrumentHosts", "#instrument-hosts");

          $("#create-form :input").prop("disabled", true);

          $.ajax({
            type : "POST",
            url : "rest/generate",
            data : JSON.stringify(request),
            dataType : 'json',
            contentType : "application/json; charset=utf-8",
            success : function(result) {
              $("#create-form :input").prop("disabled", false);
              $("#create-form")[0].reset();
              $("#create-status").html(
                  "<h3>Created job with UUID " + result.trackingId + "</h3>");
            },
            error : function() {
              $("#create-form :input").prop("disabled", false);
              $("#create-status").html("<b>ERROR!</b> Check logs.");
            }
          });
        });
  </script>
	<!-- ........GENERATE NEW MOSAIC....................................................................................... -->
	<!-- .................................................................................................................. -->
	<!-- .................................................................................................................. -->


	<!-- .................................................................................................................. -->
	<!-- .................................................................................................................. -->
	<!-- ........CHECK JOB STATE........................................................................................... -->
	<h2>Check status of an existing job</h2>
	<table>
		<tr>
			<td>Job UUID:</td>
			<td><input id="job-uuid" /></td>
			<td><input id="check-button" type="button" value="Check" /></td>
			<td><div id="job-status-wrapper">
					<table>
						<tr>
							<td>Status:</td>
							<td><div id="job-status" /></td>
						</tr>
						<tr>
							<td>Reason:</td>
							<td><div id="job-reason" /></td>
						</tr>
						<tr>
							<td>Link:</td>
							<td><div id="job-link" /></td>
						</tr>
					</table>
				</div></td>
		</tr>
	</table>

	<script type='text/javascript'>
    $("#check-button").click(
        function(event) {
          $('#job-status-wrapper div').html('');

          $.ajax({
            type : "GET",
            url : "rest/status/" + $('#job-uuid').val(),
            dataType : 'json',
            contentType : "application/json; charset=utf-8",
            success : function(resp) {
              $('#job-status').text(resp.status);
              $('#job-reason').text(resp.reason);

              if (resp.link) {
                $('#job-link').html(
                    "<a href='" + resp.link + "'>" + resp.link + "</a>");
              }
            },
            error : function(result, textStatus, thrownError) {
              resp = JSON.parse(result.responseText);
              $('#job-status').text(resp.status);
              $('#job-reason').text(resp.reason);
            }
          });
        });
  </script>
	<!-- ........CHECK JOB STATE........................................................................................... -->
	<!-- .................................................................................................................. -->
	<!-- .................................................................................................................. -->

</body>
</html>
