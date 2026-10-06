package com.ainceborn.pdfbox.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ainceborn.pdfbox.Loader;
import com.ainceborn.pdfbox.android.PDFBoxConfig;
import com.ainceborn.pdfbox.pdmodel.PDDocument;
import com.ainceborn.pdfbox.pdmodel.PDDocumentCatalog;
import com.ainceborn.pdfbox.pdmodel.PDPage;
import com.ainceborn.pdfbox.pdmodel.PDPageContentStream;
import com.ainceborn.pdfbox.pdmodel.encryption.AccessPermission;
import com.ainceborn.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import com.ainceborn.pdfbox.pdmodel.font.PDFont;
import com.ainceborn.pdfbox.pdmodel.font.PDType1Font;
import com.ainceborn.pdfbox.pdmodel.font.Standard14Fonts;
import com.ainceborn.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.ainceborn.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.ainceborn.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.ainceborn.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.ainceborn.pdfbox.pdmodel.interactive.form.PDCheckBox;
import com.ainceborn.pdfbox.pdmodel.interactive.form.PDComboBox;
import com.ainceborn.pdfbox.pdmodel.interactive.form.PDField;
import com.ainceborn.pdfbox.pdmodel.interactive.form.PDListBox;
import com.ainceborn.pdfbox.pdmodel.interactive.form.PDRadioButton;
import com.ainceborn.pdfbox.pdmodel.interactive.form.PDTextField;
import com.ainceborn.pdfbox.rendering.ImageType;
import com.ainceborn.pdfbox.rendering.PDFRenderer;
import com.ainceborn.pdfbox.text.PDFTextStripper;
import com.ainceborn.pdfbox.android.PDFBoxResourceLoader;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class MainActivity extends Activity {

    enum PdfAsset {
        FORM_TEST("FormTest.pdf"),
        HELLO("Hello.pdf"),
        MANUAL("manual.pdf"),
        MANUAL_2("manual_2.pdf"),
        WCP_FORM_BEFORE_CHANGE("WCPForm_Before change.pdf"),
        D2000_CLOSURE_DRWG("D2000 20Inch Closure DRWG.pdf"),
        IMMIGRATION_ACT("ImmigrationAct.pdf"),
        PDF_TEST("pdf-test.pdf"),
        PREVIEW("preview.pdf"),
        DOC_105_A4("105-1.-A4-.-AC8Z11MS.pdf");

        final String fileName;

        PdfAsset(String fileName) {
            this.fileName = fileName;
        }
    }

    File root;
    AssetManager assetManager;
    Bitmap pageImage;
    TextView tv;
    ProgressBar progressBar;
    volatile boolean isRendering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        setup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Initializes variables used for convenience
     */
    private void setup() {
        // Enable Android asset loading
        PDFBoxResourceLoader.init(getApplicationContext());
        PDFBoxConfig.setFontLoadLevel(PDFBoxConfig.FontLoadLevel.FULL);
        // Find the root of the external storage.

        root = getApplicationContext().getCacheDir();
        assetManager = getAssets();
        tv = (TextView) findViewById(R.id.statusTextView);
        progressBar = (ProgressBar) findViewById(R.id.renderProgressBar);
    }

    /**
     * Creates a new PDF from scratch and saves it to a file
     */
    public void createPdf(View v) {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        // Create a new font object selecting one of the PDF base fonts
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        // Or a custom font
//        try
//        {
//            // Replace MyFontFile with the path to the asset font you'd like to use.
//            // Or use LiberationSans "com/ainceborn/pdfbox/resources/ttf/LiberationSans-Regular.ttf"
//            font = PDType0Font.load(document, assetManager.open("MyFontFile.TTF"));
//        }
//        catch (IOException e)
//        {
//            Log.e("PdfBox-Android-Sample", "Could not load font", e);
//        }

        PDPageContentStream contentStream;

        try {
            // Define a content stream for adding to the PDF
            contentStream = new PDPageContentStream(document, page);

            // Write Hello World in blue text
            contentStream.beginText();
            contentStream.setNonStrokingColor(15, 38, 192);
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello World");
            contentStream.endText();

            // Load in the images
            InputStream in = assetManager.open("falcon.jpg");
            InputStream alpha = assetManager.open("trans.png");

            // Draw a green rectangle
            contentStream.addRect(5, 500, 100, 100);
            contentStream.setNonStrokingColor(0, 255, 125);
            contentStream.fill();

            // Draw the falcon base image
            PDImageXObject ximage = JPEGFactory.createFromStream(document, in);
            contentStream.drawImage(ximage, 20, 20);

            // Draw the red overlay image
            Bitmap alphaImage = BitmapFactory.decodeStream(alpha);
            PDImageXObject alphaXimage = LosslessFactory.createFromImage(document, alphaImage);
            contentStream.drawImage(alphaXimage, 20, 20 );

            // Make sure that the content stream is closed:
            contentStream.close();

            // Save the final pdf document to a file
            String path = root.getAbsolutePath() + "/Created.pdf";
            document.save(path);
            document.close();
            tv.setText("Successfully wrote PDF to " + path);

        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while creating PDF", e);
        }
    }

    /**
     * Loads an existing PDF and renders it to a Bitmap
     */
    public void renderFile(View v) {
        if (isRendering) return;

        PdfAsset[] assets = PdfAsset.values();
        String[] names = new String[assets.length];
        for (int i = 0; i < assets.length; i++) {
            names[i] = assets[i].fileName;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select PDF to render")
                .setItems(names, (dialog, which) -> openAndRenderPdf(assets[which]))
                .show();
    }

    private void openAndRenderPdf(PdfAsset asset) {
        try {
            PDDocument document = Loader.loadPDF(assetManager.open(asset.fileName));
            PDFRenderer renderer = new PDFRenderer(document);
            AtomicInteger pageIndex = new AtomicInteger(0);

            ImageView imageView = (ImageView) findViewById(R.id.renderedImageView);
            imageView.setOnClickListener(view -> renderPageInBackground(renderer, pageIndex, pageIndex.getAndIncrement()));
            imageView.setOnLongClickListener(view -> {
                renderPageInBackground(renderer, pageIndex, pageIndex.decrementAndGet());
                return true;
            });

            renderPageInBackground(renderer, pageIndex, pageIndex.getAndIncrement());
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while opening file", e);
        }
    }

    private void renderPageInBackground(PDFRenderer renderer, AtomicInteger pageIndex, int page) {
        if (isRendering) return;
        isRendering = true;
        setRenderingUiState(true);

        new Thread(() -> {
            Bitmap result = null;
            try {
                result = renderer.renderImage(page, 1, ImageType.ARGB);
            } catch (Throwable e) {
                Log.e("PdfBox-Android-Sample", "Exception thrown while rendering page " + page, e);
                // revert page index on error so next tap retries the same page
                pageIndex.set(page);
            }
            final Bitmap bitmap = result;
            runOnUiThread(() -> {
                isRendering = false;
                setRenderingUiState(false);
                if (bitmap != null) {
                    pageImage = bitmap;
                    ImageView imageView = (ImageView) findViewById(R.id.renderedImageView);
                    imageView.setImageBitmap(pageImage);
                }
            });
        }).start();
    }

    private void setRenderingUiState(boolean rendering) {
        progressBar.setVisibility(rendering ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonRender).setEnabled(!rendering);
        findViewById(R.id.buttonCreate).setEnabled(!rendering);
        findViewById(R.id.buttonFillForm).setEnabled(!rendering);
        findViewById(R.id.buttonStripText).setEnabled(!rendering);
        findViewById(R.id.buttonCreateEncrypted).setEnabled(!rendering);
    }

    /**
     * Fills in a PDF form and saves the result
     */
    public void fillForm(View v) {
        try {
            // Load the document and get the AcroForm
            PDDocument document = Loader.loadPDF(assetManager.open("sap_test.pdf"));
            PDDocumentCatalog docCatalog = document.getDocumentCatalog();
            PDAcroForm acroForm = docCatalog.getAcroForm();

            // Fill the text field
            PDTextField field = (PDTextField) acroForm.getField("TextField");
            field.setValue("Filled Text Field");
            // Optional: don't allow this field to be edited
            field.setReadOnly(true);

            PDField checkbox = acroForm.getField("Checkbox");
            ((PDCheckBox) checkbox).check();

            PDField radio = acroForm.getField("Radio");
            ((PDRadioButton)radio).setValue("Second");

            PDField listbox = acroForm.getField("ListBox");
            List<Integer> listValues = new ArrayList<>();
            listValues.add(1);
            listValues.add(2);
            ((PDListBox) listbox).setSelectedOptionsIndex(listValues);

            PDField dropdown = acroForm.getField("Dropdown");
            ((PDComboBox) dropdown).setValue("Hello");

            String path = root.getAbsolutePath() + "/FilledForm.pdf";
            tv.setText("Saved filled form to " + path);
            document.save(path);
            document.close();
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while filling form fields", e);
        }
    }

    /**
     * Strips the text from a PDF and displays the text on screen
     */
    public void stripText(View v) {
        String parsedText = null;
        PDDocument document = null;
        try {
            document = Loader.loadPDF(assetManager.open("Hello.pdf"));
        } catch(IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while loading document to strip", e);
        }

        try {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(0);
            pdfStripper.setEndPage(1);
            parsedText = "Parsed text: " + pdfStripper.getText(document);
        }
        catch (IOException e)
        {
            Log.e("PdfBox-Android-Sample", "Exception thrown while stripping text", e);
        } finally {
            try {
                if (document != null) document.close();
            }
            catch (IOException e)
            {
                Log.e("PdfBox-Android-Sample", "Exception thrown while closing document", e);
            }
        }
        tv.setText(parsedText);
    }

    /**
     * Creates a simple pdf and encrypts it
     */
    public void createEncryptedPdf(View v)
    {
        String path = root.getAbsolutePath() + "/crypt.pdf";

        int keyLength = 128; // 128 bit is the highest currently supported

        // Limit permissions of those without the password
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(false);

        // Sets the owner password and user password
        StandardProtectionPolicy spp = new StandardProtectionPolicy("12345", "hi", ap);

        // Setups up the encryption parameters
        spp.setEncryptionKeyLength(keyLength);
        spp.setPermissions(ap);
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();

        document.addPage(page);

        try
        {
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Write Hello World in blue text
            contentStream.beginText();
            contentStream.setNonStrokingColor(15, 38, 192);
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello World");
            contentStream.endText();
            contentStream.close();

            // Save the final pdf document to a file
            document.protect(spp); // Apply the protections to the PDF
            document.save(path);
            document.close();
            tv.setText("Successfully wrote PDF to " + path);

        }
        catch (IOException e)
        {
            Log.e("PdfBox-Android-Sample", "Exception thrown while creating PDF for encryption", e);
        }
    }

}