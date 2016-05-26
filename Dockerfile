FROM transgressoft/debian-java8-maven

WORKDIR /code

# Add sources, download dependencies, compile

ADD src /code/src

# docker run -v /tmp/.X10-unix:/tmp/.X11-unix -e DISPLAY=unix$DISPLAY {image_name}

