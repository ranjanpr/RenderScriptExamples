//
// Created by Alberto on 28/02/2016.
//

#include "main.h"

#include "ScriptC_main.h"

using namespace android::RSC;

sp<RS> mRS;

// Initialize RenderScript context
void initRS(const char* cacheDir)
{

	LOGD("NDK: creating new RS context");
    mRS = new RS();
	LOGD("NDK: initializing new RS context with cache dir: %s", cacheDir);
    mRS->init(cacheDir);
}

// Debug function that copies allocation contents and print it
void debugAllocationSimpleCopy(char* tag, sp<Allocation> dAllocation)
{

    // Retrieve elements count
    const int xSize = dAllocation->getType()->getX();
    
    // Copies allocation contents to local array
    int localCopy[xSize];
    dAllocation->copy1DTo((void*) localCopy);

    char debugString[255];

    // Print array
    char* pos = debugString;
    for(int i = 0; i != xSize; i++) {
        if(i) {
            pos += sprintf(pos, ", ");
        }
        pos += sprintf(pos, "%d", localCopy[i]);
    }

    LOGD("%s: %s\n", tag, debugString);
}

void runNDKExample()
{

    const int inputElementsCount = 10;

    int inputArray[inputElementsCount];

    // Fills input data with some values
    for(int i = 0; i < inputElementsCount; i++) {
        inputArray[i] = i;
    }
    
    LOGD("Filled sample input data");

    // Instantiates an Allocation and copies in it
    sp<Allocation> inputAllocationSimple = Allocation::createSized(mRS, Element::I32(mRS), inputElementsCount);
    inputAllocationSimple->copy1DFrom((void*)inputArray);

    debugAllocationSimpleCopy("inputAllocationSimple", inputAllocationSimple);

    // Instantiates an Allocation that will have RS_ALLOCATION_USAGE_SHARED flag, passing directly
    // custom data
    sp<const Type> inputType = Type::create(mRS, Element::I32(mRS), inputElementsCount, 0, 0);
    sp<Allocation> inputAllocationPointer =
        Allocation::createTyped(mRS,
                                inputType,
                                RS_ALLOCATION_MIPMAP_NONE,
                                RS_ALLOCATION_USAGE_SCRIPT | RS_ALLOCATION_USAGE_SHARED,
                                (void*)inputArray);

    debugAllocationSimpleCopy("inputAllocationPointer", inputAllocationPointer);

    // Output allocation where to store results
    sp<Allocation> outputAllocation = Allocation::createSized(mRS, Element::I32(mRS), inputElementsCount);

    // Init custom script
    ScriptC_main myScript(mRS);

    // Execute kernel on simple allocation and debug it
    myScript.forEach_mulKernel(inputAllocationSimple, outputAllocation);
    mRS->finish();

    debugAllocationSimpleCopy("inputAllocationSimple -> outputAllocation", outputAllocation);

    // Execute kernel on pointer allocation and debug it
    myScript.forEach_mulKernel(inputAllocationSimple, outputAllocation);
    mRS->finish();

    debugAllocationSimpleCopy("inputAllocationPointer -> outputAllocation", outputAllocation);
}

// JNI section

extern "C" {

JNI_FUNCTION(void, initRenderScript, jstring cacheDirObj)
{

    // Retrieve cache dir path from Java
    const char* cacheDir = env->GetStringUTFChars(cacheDirObj, nullptr);

    // Initialize RS context
    initRS(cacheDir);

    // Release string data
    env->ReleaseStringUTFChars(cacheDirObj, cacheDir);
}

// Test function to invoke from Java
JNI_FUNCTION(void, ndkExample)
{
    runNDKExample();
}
}