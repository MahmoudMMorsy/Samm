cmake_minimum_required(VERSION 3.22.1)

project("retroplay")

add_library(retroplay SHARED native-lib.cpp)

# Link against the prebuilt libmgba.so
add_library(mgba SHARED IMPORTED)
set_target_properties(mgba PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libmgba.so)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

target_link_libraries(retroplay mgba ${log-lib} ${jnigraphics-lib})
