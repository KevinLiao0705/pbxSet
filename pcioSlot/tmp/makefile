CXX = g++
TARGET = bridge
SRC = bridge.cpp uvm_usart.cpp uvm_timer.cpp 
LIBS =  -lm -lwiringPi -lpthread
INC_PATH = -I./include 
OPTIONS = -g  
$(TARGET) : $(SRC)
	$(CXX) $(OPTIONS)  -o $(TARGET) $(INC_PATH) $(LIB_PATH)  $(SRC)  $(LIBS)
clean : 
	rm $(TARGET) *.o
install :
	cp -a bridge /home/pi
