#include "sunflowConstants.h"

const char* FILTER_NAMES[] = {
    "box",
    "triangle",
    "catmull-rom",
    "mitchell",
    "lanczos",
    "blackman-harris",
    "sinc",
    "gaussian",
};

const unsigned int NUM_FILTER_NAMES = sizeof(FILTER_NAMES) / sizeof(const char*);

const unsigned int DIR_LIGHT_RADIUS = 1000;
