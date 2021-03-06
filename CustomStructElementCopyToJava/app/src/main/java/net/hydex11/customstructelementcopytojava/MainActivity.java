package net.hydex11.customstructelementcopytojava;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Java";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        example();
    }

    LogView logView;

    private void example() {

        // Initialize log view
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);

        // Create a view to see LogCat log
        logView = new LogView(this, new String[]{TAG, "RenderScript"}, 5);
        logView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        logView.addLogLine("Wait for logs. It is going to take some seconds...\n");

        // Add our console view to the window
        linearLayout.addView(logView);

        // Initialize RS context
        RenderScript mRS = RenderScript.create(this);

        // Initialize RS script
        ScriptC_main scriptC_main = new ScriptC_main(mRS);

        // Create Allocation to fill
        int sizeX = 3, sizeY = 2;
        Allocation allocationGrayPointOrdered = ScriptField_GrayPointOrdered.create2D(mRS, sizeX, sizeY).getAllocation();

        // Fills allocation
        scriptC_main.forEach_fillRandom(allocationGrayPointOrdered);

        // Debug RS elements
        scriptC_main.invoke_debugElements(allocationGrayPointOrdered, sizeX, sizeY);

        //--- Copy process
        // Define destination array
        byte destinationArray[] = new byte[allocationGrayPointOrdered.getBytesSize()];

        // Gets reflected method
        Method copyToWithoutValidationMethod = getCopyToWithoutValidationMethod();

        // Tries to copy contents
        try {
            copyToWithoutValidationMethod.invoke(allocationGrayPointOrdered,
                    destinationArray, Element.DataType.UNSIGNED_8,
                    destinationArray.length);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        // Defines destination array
        ScriptField_GrayPointOrdered.Item mappedItems[][] =
                new ScriptField_GrayPointOrdered.Item[sizeX][sizeY];

        // Wraps array contents
        ByteBuffer byteBuffer = ByteBuffer.wrap(destinationArray);
        // Sets byte order to be Android-like
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Iterates on every column and row
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {

                // Allocates a new item
                ScriptField_GrayPointOrdered.Item currentItem =
                        new ScriptField_GrayPointOrdered.Item();

                // Calculate the offset in the source array
                int currentOffset = (x + y * sizeX) * ScriptField_GrayPointOrdered.Item.sizeof;

                // Gets data from the byte array
                currentItem.x = byteBuffer.getInt(currentOffset);
                currentItem.y = byteBuffer.getInt(currentOffset + 4);
                currentItem.a = byteBuffer.getShort(currentOffset + 8);
                currentItem.b = byteBuffer.get(currentOffset + 10);

                mappedItems[x][y] = currentItem;
            }
        }

        // Debugs elements
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                ScriptField_GrayPointOrdered.Item item = mappedItems[x][y];
                Log.d(TAG, String.format("(%d,%d): x=%d, y=%d, a=%d, b=%d",
                        x, y, item.x, item.y, item.a, item.b));
            }
        }

    }

    private static Method getCopyToWithoutValidationMethod() {
        // private void copyTo(Object array, Element.DataType dt, int arrayLen)
        Method allocationHiddenCopyToMethod = null;
        try {
            allocationHiddenCopyToMethod = Allocation.class.getDeclaredMethod("copyTo",
                    Object.class, Element.DataType.class, int.class);
            allocationHiddenCopyToMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find allocationHiddenCopyToMethod");
        }

        return allocationHiddenCopyToMethod;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (logView != null) {
            logView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (logView != null) {
            logView.onPause();
        }
    }

}
