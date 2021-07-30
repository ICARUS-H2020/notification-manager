FROM openjdk:8
COPY ./src/main/java/aero/icarus2020/models/EmailTemplate.html /home/EmailTemplate.html
COPY ./target/icarus-email-mod-prod.jar /home/icarus-email-mod.jar
CMD ["java","-jar", "/home/icarus-email-mod.jar"]
