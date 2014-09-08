NTL-Lunar-Mapping-and-Modeling-Portal
=====================================

The Lunar Mapping and Modeling Portal (LMMP) is a system that has been built to support lunar exploration activities that will enable return of both manned and unmanned missions to the Moon. It provides a web-based Portal and a suite of interactive visualization and analysis tools to enable mission planners, lunar scientists, and engineers to access mapped lunar data products from past and current lunar missions. It also addresses the lunar science community, the lunar commercial community, education and public outreach (E/PO), and anyone else interested in accessing or utilizing lunar data. 



1) Read and follow instructions in SETUP.txt

2a) To deploy locally:

    mvn clean package
    cp target/lmmp-rest*.war /PATH/TO/DEPLOYMENT/FOLDER
  
    
2b) To run in place:
    
    su - hadoop

    (remember to start Hadoop according to ~demo/README.md!)

    mr-jobhistory-daemon.sh start historyserver

    mvn -Phadoop-tomcat tomcat:run -Dmaven.tomcat.port=8181

3)  It appears that the EC2 Security Group does NOT allow port 8181 to be
    accessed externally. Please make sure you are running a local tunnel
    from the command line like so:
    
    ssh -L 8181:localhost:8181 ubuntu@your-vm-ip.ec2.amazonaws.com
    
    If you do not do this, the URLs below will not work.
    
4)  Open your browser to http://localhost:8181/lmmp-rest
