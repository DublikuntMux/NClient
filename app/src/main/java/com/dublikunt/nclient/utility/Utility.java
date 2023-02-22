package com.dublikunt.nclient.utility;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.dublikunt.nclient.R;
import com.dublikunt.nclient.settings.Global;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Random;

public class Utility {
    public static final Random RANDOM = new Random(System.nanoTime());
    public static final String ORIGINAL_URL = "nhentai.net";

    /**
     * Returns the base URL for the API. This is based on the host and port specified in the configuration.
     *
     * @return base URL for the API including the port and port specified in the configuration. Note that the host will be added
     */
    public static String getBaseUrl() {
        return "https://" + Utility.getHost() + "/";
    }


    /**
     * Returns the host that the application is running on. This is used to distinguish between different instances of Jitsi's host - based application.
     *
     * @return the host that the application is running on or null if it isn't running on a host -
     */
    public static String getHost() {
        return Global.getMirror();
    }

    /**
     * Parses an escaped character and writes it to the writer. Escaped characters are U + 0000 - FFFF followed by hexadecimal digits in the range 0 - 9A - F.
     *
     * @param reader - the reader to read from. Not null.
     * @param writer - the writer to write to. Not null. The result is written to
     */
    private static void parseEscapedCharacter(Reader reader, Writer writer) throws IOException {
        int toCreate, read;
        // Read the next character from the input.
        switch (read = reader.read()) {
            case 'u':
                toCreate = 0;
                // Reads 4 bytes from the reader.
                for (int i = 0; i < 4; i++) {
                    toCreate *= 16;
                    toCreate += Character.digit(reader.read(), 16);
                }
                writer.write(toCreate);
                break;
            case 'n':
                writer.write('\n');
                break;
            case 't':
                writer.write('\t');
                break;
            default:
                writer.write('\\');
                writer.write(read);
                break;
        }
    }

    /**
     * Unescapes unicode characters in script HTML. This is useful for scripts that are embedded in HTML such as a table or script tag.
     *
     * @param scriptHtml - The script HTML to unescape. May be null in which case " " is returned.
     * @return The unescaped script HTML or " " if there was an error in the input string which could be a string
     */
    @NonNull
    public static String unescapeUnicodeString(@Nullable String scriptHtml) {
        // Returns the scriptHtml if scriptHtml is not null.
        if (scriptHtml == null) return "";
        StringReader reader = new StringReader(scriptHtml);
        StringWriter writer = new StringWriter();
        int actualChar;
        try {
            // Reads the next character from the reader.
            while ((actualChar = reader.read()) != -1) {
                // If the actual character is not escaped then the actual character is written to the writer.
                if (actualChar != '\\') writer.write(actualChar);
                else parseEscapedCharacter(reader, writer);
            }
        } catch (IOException ignore) {
            return "";
        }
        return writer.toString();
    }

    /**
     * Sleeps for the specified amount of time. This is useful for unit testing and to ensure that the test is running in a safe environment.
     *
     * @param millis - the amount of time to sleep in milliseconds
     */
    public static void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tints the menu to a given size. This is useful for toggling the tint on / off in order to make it easier to read and / or modify the menu while it is running
     *
     * @param menu - The menu to tint
     */
    public static void tintMenu(Menu menu) {
        int x = menu.size();
        // Set the Tint to the global Tint
        for (int i = 0; i < x; i++) {
            MenuItem item = menu.getItem(i);
            Global.setTint(item.getIcon());
        }
    }

    /**
     * Converts a Drawable to a Bitmap. This is useful for determining which type of drawable to use as a source for an android. graphics. Bitmap.
     *
     * @param dra - The Drawable to convert. Must be a BitmapDrawable.
     * @return The Bitmap or null if the Drawable is not a BitmapDrawable or does not implement { @link android. graphics. Bitmap
     */
    @Nullable
    private static Bitmap drawableToBitmap(Drawable dra) {
        // Returns the drawable if it s a BitmapDrawable.
        if (!(dra instanceof BitmapDrawable)) return null;
        return ((BitmapDrawable) dra).getBitmap();
    }

    /**
     * Saves the given Drawable to the given File. This will be converted to a bitmap before saving. If you want to save to a file that already exists use #saveImage ( Bitmap File ) instead.
     *
     * @param drawable - The Drawable to save. It will be converted to a bitmap then saved.
     * @param output   - The File to save to. This should be a file
     */
    public static void saveImage(Drawable drawable, File output) {
        Bitmap b = drawableToBitmap(drawable);
        // Save the image to the output file.
        if (b != null) saveImage(b, output);
    }

    /**
     * Saves the given bitmap to the given file. This is a convenience method for use in testing. It compresses the image to JPEG and saves it to the given file.
     *
     * @param bitmap - The bitmap to save. Must not be null.
     * @param output - The file to save the bitmap to. Must not be null
     */
    private static void saveImage(@NonNull Bitmap bitmap, @NonNull File output) {
        try {
            // Create a new file if it doesn t exist.
            if (!output.exists()) output.createNewFile();
            FileOutputStream ostream = new FileOutputStream(output);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, ostream);
            ostream.flush();
            ostream.close();
        } catch (IOException e) {
            LogUtility.INSTANCE.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Writes the contents of an input stream to a file. Closes the input stream when done. This method is useful for debugging purposes as it does not create a File object at all.
     *
     * @param inputStream - The input stream to write. Must not be null.
     * @param filePath    - The file to write to. Must not be null
     */
    public static long writeStreamToFile(InputStream inputStream, File filePath) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(filePath);
        int read;
        long totalByte = 0;
        byte[] bytes = new byte[1024];
        // Read bytes from the input stream and write them to the output stream.
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
            totalByte += read;
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
        return totalByte;
    }

    /**
     * Sends an image to the user. This is a convenience method for sending images that are in the app's android. app. Activity hierarchy.
     *
     * @param context  - The context to use. Must be non - null.
     * @param drawable - The image to send. Must be non - null.
     * @param text     - The text to send. If null no text is sent
     */
    public static void sendImage(Context context, Drawable drawable, String text) {
        context = context.getApplicationContext();
        try {
            File tempFile = File.createTempFile("toSend", ".jpg");
            tempFile.deleteOnExit();
            Bitmap image = drawableToBitmap(drawable);
            // Returns the image if it is not null.
            if (image == null) return;
            saveImage(image, tempFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            // put the text to the share intent
            if (text != null) shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            Uri x = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", tempFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, x);
            shareIntent.setType("image/jpeg");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, x, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            shareIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_with));
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shareIntent);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
