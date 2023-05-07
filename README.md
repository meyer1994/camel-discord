# Camel Discord Component

[![build](https://github.com/meyer1994/camel-discord/actions/workflows/build.yml/badge.svg)](https://github.com/meyer1994/camel-discord/actions/workflows/build.yml)
[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

This was way harder to do than it should have been

## Table of Contents

- [About](#about)
- [Install](#install)
- [Usage](#usage)
- [Thanks](#thanks)

## About

Once I wanted to create a Discord bot that could be used as an interface to 
many other things. Reboot an EC2 instance on AWS, send an e-mail, etc. 

[Apache Camel][1] is an integrations library. Adding a Discord component to it 
makes Discord very powerful. This is my attempt on creating a component.

## Install

To build this project use

```sh 
$ mvn install
```

### Gradle

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
        implementation 'com.github.meyer1994:camel-discord:0.0.2'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.meyer1994</groupId>
    <artifactId>camel-discord</artifactId>
    <version>0.0.2</version>
</dependency>
```

## Usage

You only need to define a `@Bean`, or use the Camel's `@BindToRegistry` 
annotation, to be autowired into the component:

```java
@Bean  // Or @BindToRegistry
public JDA jda() throws LoginException {
    return JDABuilder.createDefault("YOUR_DISCORD_BOT_TOKEN")
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .build();
}
```

And now you can use the `discord` route. The example below shows a simple 
_ping pong_ bot.

```java
from("discord:ping")
        .filter().simple("${body.contentRaw} == '!ping'")
        .transform().constant("Pong!")
        .to("discord:pong");
```

## Thanks

[AWS'][2] components. It served a very useful learning resource;
    
[1]: https://camel.apache.org/
[2]: https://github.com/apache/camel/tree/main/components/camel-aws