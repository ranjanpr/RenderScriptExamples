// FilterScript script

#pragma version(1)

#pragma rs java_package_name(net.hydex11.profilerexample)

// Some redit goes to: http://stackoverflow.com/questions/13917106/where-is-the-filterscript-documentation-and-how-can-i-use-it

int blurRadius;

rs_allocation inputAllocation;
rs_allocation outputAllocation;

uint32_t width;
uint32_t height;

uchar4 __attribute__((kernel)) blurSimpleKernelFS(uint32_t x, uint32_t y) {
    uint4 sum = 0;
    uint count = 0;
    for (int yi = -blurRadius; yi <= blurRadius; ++yi) {
        for (int xi = -blurRadius; xi <= blurRadius; ++xi) {
                sum += convert_uint4(rsGetElementAt_uchar4(inputAllocation, x+xi, y+yi));
                ++count;
        }
    }
    return convert_uchar4(sum/count);
}

// Test an allocation directly loaded inside a script
const int pngWidth = 500;
const int pngHeight = 286;
uchar4 pngData[pngWidth * pngHeight];

void __attribute__((kernel)) fillPngData(uchar4 in, uint32_t x, uint32_t y){
    pngData[x + y * pngWidth] = in;
}

uchar4 __attribute__((kernel)) blurSimpleKernelFSGetFromScriptVariable(uint32_t x, uint32_t y) {
    uint4 sum = 0;
    uint count = 0;

    // We can use the image width as stride
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
    {
        for (int xi = -blurRadius; xi <= blurRadius; ++xi) {

            int idx = x+xi + (y+yi) * pngWidth;
            sum += convert_uint4(pngData[idx]);
            ++count;
        }
    }

    return convert_uchar4(sum/count);
}

// Set tons of values, using radius blur
void __attribute__((kernel)) setValuesSimpleKernelFS(uchar4 in, int x, int y){
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
        {
            for (int xi = -blurRadius; xi <= blurRadius; ++xi) {
                rsSetElementAt_uchar4(outputAllocation, in, x+xi, y+yi);
            }
        }
}

const static float3 grayMultipliers = {0.299f, 0.587f, 0.114f};

// The following function presents a standard way to peroform in/out
// operations on Allocation having different Types.
uchar __attribute__((kernel)) rgbaToGraySimpleKernelFS(uchar4 in, uint32_t x, uint32_t y) {

    return (uchar) ((float)in.r*grayMultipliers.r +
                      (float)in.g*grayMultipliers.g +
                    (float)in.b*grayMultipliers.b);
}