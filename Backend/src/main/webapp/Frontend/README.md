# Frontend

Easiest way to use is with thttpd.

0) Requirements: node.js, npm, make
1) Download thttpd (http://acme.com/software/thttpd/)
2) Install react-tools via npm and make sure it's in your PATH:
   > sudo npm install -g react-tools
3) Compile the jsx files:
   > make
4) Start a static server in the directory:
   > thttpd -d ./ -p 8000

5) Open web browser and go to http://localhost:8000