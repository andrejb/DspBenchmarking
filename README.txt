DSP Benchmarking
================

This is an Android project developed at IME/USP for a masters course on
Computer Science. The main purpose is to run real time audio processing
algorithms, collect data about devices performance, and send that data back to
the authors through email.


Compiling from source
---------------------

The following steps should be sufficient to compile the application from source:

1. Install Eclipse and ADT Plugin [1]. Install Android SDK API 7 by choosing
   "Window" -> "Android SDK Manager" option in Eclipse.

   [1] https://developer.android.com/tools/sdk/eclipse-adt.html

2. Download and extract Android NDK [2]. This is needed to run some tests
   which are based on C/C++ code, as for example the FFTW library.

   [2] https://developer.android.com/tools/sdk/ndk/index.html

3. Clone the source code from github:

     git clone git://github.com/andrejb/DspBenchmarking
     cd DspBenchmarking
     git checkout develop

4. Import the source code in Eclipse:

    - Create a new Android Project.
    - Import the cloned source code into the workspace.

5. You will probably need to fix the path to NDK inside Eclipse:

    - Go to "Project" -> "Properties" -> "Builders".
    - In the Builders list, remove any builders that show an error.
    - Choose "New" -> "Program".
    - In "Main" tab, fill "location" wwith the path for the "ndk-build"
      executable script that comes with Android NDK. 
    - Also in "Main" tab, fill "working directory" with the path for the
      "jni/" folder inside the project.
    - On "Refresh" tab, mark "Refresh sources upon completion", "The entire
      workspace", and "recursivelly include subfolders".


Authorship
----------

* Andre Jucovsky Bianchi.
* http://www.ime.usp.br/~ajb
* 2012-2013.
