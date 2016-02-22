// RenderScript version

#pragma version(1)
#pragma rs_fp_relaxed

#pragma rs java_package_name(net.hydex11.profilerexample)

// Some credit goes to: http://stackoverflow.com/questions/13917106/where-is-the-filterscript-documentation-and-how-can-i-use-it

int blurRadius = 3;

rs_allocation inputAllocation;
rs_allocation outputAllocation;
rs_allocation grayAllocation;

uint32_t width = 0;
uint32_t height = 0;

// Blur function
uchar4 __attribute__((kernel)) blurSimpleKernel(uint32_t x, uint32_t y) {
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

void blurPointerKernel(const uchar4 * v_in, uchar4 * v_out, uint32_t x, uint32_t y) {
    uint4 sum = 0;
    uint count = 0;

    // We can use the image width as stride
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
    {
        for (int xi = -blurRadius; xi <= blurRadius; ++xi) {
            sum += convert_uint4(v_in[xi + width * yi]);
            ++count;
        }
    }

    *v_out = convert_uchar4(sum/count);
}

void blurPointerKernelSet(const uchar4 * v_in, uint32_t x, uint32_t y) {
    uint4 sum = 0;
    uint count = 0;

    // We can use the image width as stride
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
    {
        for (int xi = -blurRadius; xi <= blurRadius; ++xi) {
            sum += convert_uint4(v_in[xi + width * yi]);
            ++count;
        }
    }

    rsSetElementAt_uchar4(outputAllocation, convert_uchar4(sum/count), x, y);
}


void blurPointerKernelGet(uchar4 * v_out, uint32_t x, uint32_t y) {
    uint4 sum = 0;
    uint count = 0;

    // We can use the image width as stride
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
    {
        for (int xi = -blurRadius; xi <= blurRadius; ++xi) {
            sum += convert_uint4(rsGetElementAt_uchar4(inputAllocation, x+xi, y+yi));
            ++count;
        }
    }

    *v_out = convert_uchar4(sum/count);
}

// Test an allocation directly loaded inside a script
const int pngWidth = 500;
const int pngHeight = 286;
uchar4 pngData[pngWidth * pngHeight];
uchar4 * pngDataPointer = &pngData;

// Kernel function used to load image data
void fillPngData(const uchar4 * v_in, uint32_t x, uint32_t y){
    pngData[x + y * pngWidth] = *v_in;
}

void blurPointerKernelGetFromScriptVariable(uchar4 * v_out, uint32_t x, uint32_t y) {
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

    *v_out = convert_uchar4(sum/count);
}
void blurPointerKernelGetFromScriptVariablePointer(uchar4 * v_out, uint32_t x, uint32_t y) {
    uint4 sum = 0;
    uint count = 0;

    // We can use the image width as stride
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
    {
        for (int xi = -blurRadius; xi <= blurRadius; ++xi) {

            int idx = x+xi + (y+yi) * pngWidth;
            sum += convert_uint4(pngDataPointer[idx]);
            ++count;
        }
    }

    *v_out = convert_uchar4(sum/count);
}

// Square set values
// Following functions set a predefined count of values in the output allocation,
// using the input as element to set.

// Uses blur radius as input number of values to set, to resemble (more or less) its
// memory access cost.

void __attribute__((kernel)) setValuesSimpleKernel(uchar4 in, int x, int y){
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
        {
            for (int xi = -blurRadius; xi <= blurRadius; ++xi) {
                rsSetElementAt_uchar4(outputAllocation, in, x+xi, y+yi);
            }
        }
}

void setValuesPointerKernel(const uchar4 * v_in, uchar4 * v_out, int x, int y){
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
        {
            for (int xi = -blurRadius; xi <= blurRadius; ++xi) {
                v_out[xi + yi*width] = *v_in;
            }
        }
}
void setValuesPointerKernelSet(const uchar4 * v_in, int x, int y){
    for(int yi = -blurRadius; yi <= blurRadius; ++yi)
        {
            for (int xi = -blurRadius; xi <= blurRadius; ++xi) {
                rsSetElementAt_uchar4(outputAllocation, *v_in, x+xi, y+yi);
            }
        }
}

// RGBA to GRAY conversion
// The following functions are used to compare performance, in the case
// where input and output Allocations have different Type.
// Following is a comparison of three standard ways to perform the in/out
// operations.

const static float3 grayMultipliers = {0.299f, 0.587f, 0.114f};

// The following function presents a standard way to peroform in/out
// operations on Allocation having different Types.
uchar __attribute__((kernel)) rgbaToGrayNoPointer(uchar4 in, uint32_t x, uint32_t y) {

    return (uchar) ((float)in.r*grayMultipliers.r +
            (float)in.g*grayMultipliers.g +
          (float)in.b*grayMultipliers.b);
}

// Input: pointer
// Output: rsSetElementAt
void rgbaToGrayPointerAndSet(const uchar4 * v_in, uint32_t x, uint32_t y)
{
    uchar out = (uchar) ((float)v_in->r*grayMultipliers.r +
            (float)v_in->g*grayMultipliers.g +
          (float)v_in->b*grayMultipliers.b);

   rsSetElementAt_uchar(grayAllocation,out,x,y);
}

// Input: rsGetElementAt
// Output: pointer
void rgbaToGrayPointerAndGet(uchar * v_out, uint32_t x, uint32_t y)
{
    uchar4 in = rsGetElementAt_uchar4(inputAllocation, x, y);

   *v_out = (uchar) ((float)in.r*grayMultipliers.r +
                        (float)in.g*grayMultipliers.g +
                      (float)in.b*grayMultipliers.b);
}

// Input: pointer
// Output: pointer
void rgbaToGrayPointerAndOut(const uchar4 * v_in, uchar * v_out, uint32_t x, uint32_t y)
{
   *v_out = (uchar) ((float)v_in->r*grayMultipliers.r +
                        (float)v_in->g*grayMultipliers.g +
                      (float)v_in->b*grayMultipliers.b);
}

// PI test
int piIterations = 30;
// Test for pure calculation
// Reference: http://www.codeproject.com/Articles/813185/Calculating-the-Number-PI-Through-Infinite-Sequenc
static float piTest() {
   float i;    // Number of iterations and control variable
  float s = 1;   //Signal for the next operation
  float pi = 3;

  for(i = 2; i <= piIterations*2; i += 2){
    pi = pi + s * (4 / (i * (i + 1) * (i + 2)));
    s = -s;
  }

  return pi;

}

float __attribute__((kernel)) PITestSimpleKernel(){

    return piTest();

}