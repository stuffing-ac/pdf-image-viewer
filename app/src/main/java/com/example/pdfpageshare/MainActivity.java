package com.example.pdfpageshare;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private LinearLayout pageList;
    private TextView status;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Uri pendingUri;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        handleIncoming(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncoming(intent);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 248, 250));
        root.setPadding(dp(16), dp(18), dp(16), dp(12));

        TextView title = new TextView(this);
        title.setText("PDF Page Share");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(25, 35, 55));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title);

        status = new TextView(this);
        status.setText("WhatsApp ya Files se PDF kholne ke liye PDF par tap karke Open with → PDF Page Share chunein.");
        status.setTextSize(14);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        pageList = new LinearLayout(this);
        pageList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(pageList);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void handleIncoming(Intent intent) {
        Uri uri = null;
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            uri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(action)) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            } else {
                //noinspection deprecation
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }
        }
        if (uri != null) loadPdf(uri);
    }

    private void loadPdf(Uri uri) {
        pendingUri = uri;
        pageList.removeAllViews();
        status.setText("PDF load ho rahi hai…");
        executor.execute(() -> {
            File local = null;
            try {
                local = new File(getCacheDir(), "incoming.pdf");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(local)) {
                    if (in == null) throw new Exception("PDF file read nahi ho paayi.");
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                }
                try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(local, ParcelFileDescriptor.MODE_READ_ONLY);
                     PdfRenderer renderer = new PdfRenderer(pfd)) {
                    int count = renderer.getPageCount();
                    runOnUiThread(() -> status.setText(count + " pages mil gaye. Kisi bhi page ko share karein."));
                    for (int i = 0; i < count; i++) {
                        final int pageNumber = i;
                        Bitmap thumb;
                        try (PdfRenderer.Page page = renderer.openPage(i)) {
                            int width = 720;
                            int height = Math.max(1, (int) (width * (page.getHeight() / (float) page.getWidth())));
                            thumb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            thumb.eraseColor(Color.WHITE);
                            page.render(thumb, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        }
                        Bitmap finalThumb = thumb;
                        runOnUiThread(() -> addPageCard(pageNumber, finalThumb));
                    }
                }
            } catch (Exception e) {
                String message = e.getMessage() == null ? "PDF open nahi ho paayi." : e.getMessage();
                runOnUiThread(() -> status.setText("Error: " + message));
            }
        });
    }

    private void addPageCard(int pageIndex, Bitmap bitmap) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(14);
        card.setElevation(dp(2));

        TextView label = new TextView(this);
        label.setText("Page " + (pageIndex + 1));
        label.setTextSize(17);
        label.setTextColor(Color.BLACK);
        label.setPadding(0, 0, 0, dp(8));
        card.addView(label);

        ImageView preview = new ImageView(this);
        preview.setImageBitmap(bitmap);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(preview, new LinearLayout.LayoutParams(-1, -2));

        Button share = new Button(this);
        share.setText("Share Page " + (pageIndex + 1));
        share.setOnClickListener(v -> sharePage(pageIndex));
        card.addView(share);
        pageList.addView(card, cardParams);
    }

    private void sharePage(int pageIndex) {
        Uri source = pendingUri;
        if (source == null) {
            Toast.makeText(this, "Pehle PDF open karein.", Toast.LENGTH_SHORT).show();
            return;
        }
        status.setText("Page image taiyar ho rahi hai…");
        executor.execute(() -> {
            try {
                File incoming = new File(getCacheDir(), "incoming.pdf");
                File dir = new File(getCacheDir(), "pages");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("Image folder nahi ban paaya.");
                File output = new File(dir, "page_" + (pageIndex + 1) + ".png");
                try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(incoming, ParcelFileDescriptor.MODE_READ_ONLY);
                     PdfRenderer renderer = new PdfRenderer(pfd);
                     PdfRenderer.Page page = renderer.openPage(pageIndex)) {
                    int width = Math.min(page.getWidth() * 2, 3000);
                    int height = Math.max(1, (int) (width * (page.getHeight() / (float) page.getWidth())));
                    Bitmap full = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    full.eraseColor(Color.WHITE);
                    page.render(full, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    try (FileOutputStream out = new FileOutputStream(output)) {
                        full.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                    full.recycle();
                }
                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", output);
                runOnUiThread(() -> {
                    status.setText("Page " + (pageIndex + 1) + " share ke liye ready hai.");
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("image/png");
                    send.putExtra(Intent.EXTRA_STREAM, contentUri);
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(send, "Page " + (pageIndex + 1) + " share karein"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Share error: " + e.getMessage()));
            }
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
