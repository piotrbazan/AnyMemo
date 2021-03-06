package org.liberty.android.fantastischmemo.test.ui;
import android.content.Context;
import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import android.view.Display;
import android.view.WindowManager;

import org.liberty.android.fantastischmemo.test.TestHelper;
import org.liberty.android.fantastischmemo.ui.CardImageGetter;
import android.test.suitebuilder.annotation.SmallTest;

public class CardImageGetterTest extends AndroidTestCase {
    private int screenWidth;
	
    private CardImageGetter cardImageGetter;

    @Override
    @SuppressWarnings("deprecation")
    public void setUp() {
        Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        screenWidth = display.getWidth();
        cardImageGetter = new CardImageGetter(getContext(), TestHelper.SAMPLE_DB_PATH);
    }

    @SmallTest
    public void testSmallIconBitmapShouldNotResize() {
        int bitmapWidth = (int) (0.1 * screenWidth);
        int[] colorArray = new int[bitmapWidth * bitmapWidth];
        Bitmap b = Bitmap.createBitmap(colorArray, bitmapWidth, bitmapWidth, Bitmap.Config.ARGB_8888);
        Bitmap test = cardImageGetter.scaleBitmap(b);
        assertEquals(bitmapWidth, test.getWidth());
    }

    @SmallTest
    public void testNormalImageBitmapShouldEnlarge() {	
        int bitmapWidth = (int) (0.4 * screenWidth);
        int[] colorArray = new int[bitmapWidth * bitmapWidth];
        Bitmap b = Bitmap.createBitmap(colorArray, bitmapWidth, bitmapWidth, Bitmap.Config.ARGB_8888);
        Bitmap test = cardImageGetter.scaleBitmap(b);
        assertEquals((int)(0.6 * screenWidth), test.getWidth());
    }
	
    @SmallTest
    public void testNormalImageBitmapShouldNotResize() {
        int bitmapWidth = (int) (0.8 * screenWidth);
        int[] colorArray = new int[bitmapWidth * bitmapWidth];
        Bitmap b = Bitmap.createBitmap(colorArray, bitmapWidth, bitmapWidth, Bitmap.Config.ARGB_8888);
        Bitmap test = cardImageGetter.scaleBitmap(b);
        assertEquals(bitmapWidth, test.getWidth());
    }

    @SmallTest
    public void testLargeImageBitmapShouldShrink() {
        int bitmapWidth = (int) (1.5 * screenWidth);
        int[] colorArray = new int[bitmapWidth * bitmapWidth];
        Bitmap b = Bitmap.createBitmap(colorArray, bitmapWidth, bitmapWidth, Bitmap.Config.ARGB_8888);
        Bitmap test = cardImageGetter.scaleBitmap(b);
        assertEquals(screenWidth, test.getWidth());
    }
}
