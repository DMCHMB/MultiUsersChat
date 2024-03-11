# Use an official OpenJDK runtime as a parent image
FROM openjdk:11

# Set the working directory to /usr/src/app
WORKDIR /usr/src/app

# Copy the contents of the server directory into the container at /usr/src/app
COPY ./src/server /usr/src/app/server
COPY ./src/connection /usr/src/app/connection

# Compile Java code
RUN javac -cp . server/*.java connection/*.java

# Run the application
CMD ["java", "server.Server"]