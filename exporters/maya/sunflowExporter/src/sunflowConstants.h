#ifndef SUNFLOW_CONSTANTS_H
#define SUNFLOW_CONSTANTS_H

namespace {

// put these constants in the anonymous namespace so they won't be added to the global namespace each time they are included

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

} // namespace

#endif /* SUNFLOW_CONSTANTS_H */
