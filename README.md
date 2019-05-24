# vkgl
Variant annotator for vkgl variant sharing.
Consists of two parts:

## hgvs translator
Currently converts hgvs to vcf.
To start the annotator service:
```
> cd docker
> docker-compose up
```
To see it in action, post an array of hgvs strings to the h2v endpoint:
```
curl -H 'Content-Type: application/json' -d '["NM_000088.3:c.589G>T", "NC_000017.10:g.48275363C>A"]' 'http://localhost:1234/h2v?keep_left_anchor=False'
```

## file processing pipeline
[Spring Boot](https://spring.io/projects/spring-boot) application that uses
[Apache Camel](http://camel.apache.org/) to read input file from `src/test/resources`
and annotate the lines with vcf info retrieved from the hgvs annotator.

Results will be stored in results dir once finished.
Results with an error message will be routed to error file.

Requires JDK 8.
To run the pipeline:
```
> mvn clean spring-boot:run
```
![Pipeline overview](./vkgl.svg)
