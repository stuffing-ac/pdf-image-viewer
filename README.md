# PDF Page Share (Android)

A small Android app that can receive PDF files from Android share/open-with menus, render every page as a preview, and share any selected page as a PNG image.

## Features
- Appears as an option for PDF files in Android's Open with menu.
- Receives PDFs shared from apps such as WhatsApp.
- Shows a preview of each PDF page.
- Shares one selected page as a PNG using Android's share sheet (including WhatsApp).
- Processes the PDF locally on the device; it does not upload the PDF to a server.

## Build APK with GitHub Actions
1. Upload the contents of this project to the root of a GitHub repository, preserving folders (especially `.github/workflows`).
2. Commit the files to the `main` branch.
3. Open the repository's **Actions** tab.
4. Select **Build PDF Page Share APK** and run it, or wait for the push-triggered build.
5. Open the completed workflow run and download the `PDF-Page-Share-debug-apk` artifact.
6. Extract the artifact ZIP on the phone and install `app-debug.apk`. Android may ask permission to install from that source.

## Use
In WhatsApp, tap a PDF, choose **Open with** (or Share → PDF Page Share). The app displays page previews. Tap **Share Page N** and choose WhatsApp or another app.

Note: Android and WhatsApp may show a confirmation or app chooser. The app cannot silently send a message into a particular chat; you select the recipient in WhatsApp's share flow.
