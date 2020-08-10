#ifndef MINICAP_SIMPLE_SERVER_HPP
#define MINICAP_SIMPLE_SERVER_HPP

class SimpleServer {
public:
    SimpleServer();
    ~SimpleServer();

    int start(int port);
    int accept();

private:
    int mFd;
};

#endif
