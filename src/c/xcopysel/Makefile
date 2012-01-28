CC = gcc

CFLAGS = -g -c 
LFLAGS = -L/usr/X11R6/lib -lX11 -lXext -lXt -lXmu -lXaw

OBJECTS = xcopysel.o
.c.o  :
	$(CC) $(CFLAGS) $? 



all 	: xcopysel

# builds server 
xcopysel	: Makefile $(OBJECTS)
	$(CC) -o xcopysel $(OBJECTS) $(LFLAGS)
	
clean	:	
	rm -f xcopysel *.o
