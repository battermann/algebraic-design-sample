# Algebraic API Design Sample App

run with

    sbt "run -i . -o report.html"
    
options

    Usage: cli-options [options]
      --usage  <bool>
            Print usage and exit
      --help | -h  <bool>
            Print help message and exit
      --input | -i  <string>
            The relative or absolute path to the directory containing the .csv the files.
      --output | -o  <string>
            The name for the generated report files. Omit file extension. (e.g. -o payment-report)
      --output-format | -f  <html | pdf>
            Supported output formats: html (default), pdf.
