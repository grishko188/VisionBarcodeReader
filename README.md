# VisionBarcodeReader
Barcode reader library that provides simple and customizable BarcodeReaderView
<br/><b>Originally based on <a href ="https://github.com/googlesamples/android-vision">Google samples android-vision</a></b>
<br/>
<br/>
<b>Pre-requisites</b>
<br/>Android Play Services SDK level 26 or greater.
<br/><br/>
<b>Integration</b>
<br/>Clone or download this project, and add library .aar file
<br/>
<br/>
<b>Simple usage</b>
<br/> Add into your layout
<br/>
````
    <com.grishko188.visionlibrary.BarcodeReaderView
        android:id="@+id/barcode_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:auto_focus="true"
        app:draw_rect="true"
        app:draw_text="false"
        app:use_flash="false" />
````
<br/> Customize in your activity
<br/>

````
BarcodeReaderView barcodeReader = findViewById(R.id.barcode_view);
barcodeReader.setPlaySoundWhenScanSuccess(false);
barcodeReader.setBarcodeFormatPreInitState(Barcode.ALL_FORMATS);
barcodeReader.setBarcodeReaderListener(this);
barcodeReader.setFoundedBarcodeRectColors(Color.RED);
````
<br/>For full customization options look inside <a href="https://github.com/grishko188/VisionBarcodeReader/tree/master/app">sample project</a>