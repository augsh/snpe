cmake_minimum_required(VERSION 3.20)

project(snpe)

set(CMAKE_CXX_STANDARD 17)

add_library(${PROJECT_NAME} INTERFACE IMPORTED GLOBAL)
target_include_directories(${PROJECT_NAME}
                           INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/include/zdl)
target_link_libraries(
  ${PROJECT_NAME}
  INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/lib/x86_64-linux-clang/libHtpPrepare.so
            ${CMAKE_CURRENT_SOURCE_DIR}/lib/x86_64-linux-clang/libSNPE.so
            ${CMAKE_CURRENT_SOURCE_DIR}/lib/x86_64-linux-clang/libomp.so)

if(NOT ${OUT} STREQUAL "")
  message("Set output directory to ${OUT}")
  file(GLOB_RECURSE BINS "bin/x86_64-linux-clang/*" "*.sh")
  file(COPY ${BINS} DESTINATION ${OUT_BIN}/snpe)
  file(GLOB_RECURSE LIBS "lib/x86_64-linux-clang/*")
  file(COPY ${LIBS} DESTINATION ${OUT_LIB})
endif()
