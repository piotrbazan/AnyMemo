/*
Copyright (C) 2013 Haowen Ning

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package org.liberty.android.fantastischmemo.ui;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.liberty.android.fantastischmemo.AMEnv;
import org.liberty.android.fantastischmemo.R;

import roboguice.util.Ln;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.view.Display;
import android.view.WindowManager;

import com.google.inject.assistedinject.Assisted;


public class CardImageGetter implements ImageGetter {
    private String dbPath;
    
    private Context context;
    
    private int screenWidth;
    
    @SuppressWarnings("deprecation")
    @Inject
    public CardImageGetter (Context context, @Assisted String dbPath) {
        this.context = context;
        this.dbPath = dbPath;
        
        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        
        screenWidth = display.getWidth();
	}
    
    @Override
    public Drawable getDrawable(String source) {
        Ln.v("Source: " + source);
        String dbName = FilenameUtils.getName(dbPath);
        try {
            String[] paths = {
            /* Relative path */
            "" + dbName + "/" + source,
            /* Try the image in /sdcard/anymemo/images/dbname/myimg.png */
            AMEnv.DEFAULT_IMAGE_PATH + dbName + "/" + source,
            /* Try the image in /sdcard/anymemo/images/myimg.png */
            AMEnv.DEFAULT_IMAGE_PATH + source,
            /* Just the last part of the name */
            AMEnv.DEFAULT_IMAGE_PATH + dbName + "/" + FilenameUtils.getName(source),
            AMEnv.DEFAULT_IMAGE_PATH + FilenameUtils.getName(source)
            };
            Bitmap orngBitmap = null;
            for (String path : paths) {
                Ln.v("Try path: " + path);
                if (new File(path).exists()) {
                    orngBitmap = BitmapFactory.decodeFile(path);
                    break;
                }
            }
            /* Try the image from internet */
            if (orngBitmap == null) {
                InputStream is = (InputStream) new URL(source).getContent();
                orngBitmap = BitmapFactory.decodeStream(is);
            }

            int width = orngBitmap.getWidth();
            int height = orngBitmap.getHeight();
            int scaledWidth = width;
            int scaledHeight = height;
            
            float scaleFactor = 1.0f;
            Matrix matrix = new Matrix();
            if (width > screenWidth) {
                scaleFactor = ((float) screenWidth) / width;
                matrix.postScale(scaleFactor, scaleFactor);
                scaledWidth = (int) (width * scaleFactor);
                scaledHeight = (int) (height * scaleFactor);
            }

            if (width > 0.2 * screenWidth && width < 0.6 * screenWidth) {
                scaleFactor = (((float) screenWidth) * 0.6f) / width;
                matrix.postScale(scaleFactor, scaleFactor);
                scaledWidth = (int) (width * scaleFactor);
                scaledHeight = (int) (height * scaleFactor);
            }
            
            Bitmap resizedBitmap = Bitmap.createBitmap(orngBitmap, 0, 0,
                    width, height, matrix, true);

            @SuppressWarnings("deprecation")
            BitmapDrawable d = new BitmapDrawable(resizedBitmap);
            //d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            d.setBounds(0, 0, scaledWidth, scaledHeight);
            return d;
        } catch (Exception e) {
            Ln.e("getDrawable() Image handling error", e);
        }

        /* Fallback, display default image */
        Drawable d = context.getResources().getDrawable(R.drawable.picture);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        return d;
    }  

    public Bitmap scaleBitmap(Bitmap b) {
        int width = b.getWidth();
        int height = b.getHeight();
     
        float scaleFactor = 1.0f;
        Matrix matrix = new Matrix();
        if (width > screenWidth) {
            scaleFactor = ((float) screenWidth) / width;
            matrix.postScale(scaleFactor, scaleFactor);
        }

        if (width > 0.2 * screenWidth && width < 0.6 * screenWidth) {
            scaleFactor = (((float) screenWidth) * 0.6f) / width;
            matrix.postScale(scaleFactor, scaleFactor);
        }
     
        Bitmap resizedBitmap = Bitmap.createBitmap(b, 0, 0,
                width, height, matrix, true);
        return resizedBitmap;
    }
};
