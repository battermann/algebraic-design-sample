# Algebraic API Design Sample App

run with

    sbt "run -i . -o report.html"
    
options

    Usage: cli-options [options]
      --usage  
            Print usage and exit
      --help | -h  
            Print help message and exit
      --input | -i  <string>
            The relative or absolute path to the directory containing .csv the files.
      --output | -o  <string>
            The name for the generated report files. Please omit file extension. (e.g. -o payment-report)
