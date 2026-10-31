# __ARTIFACT__

End-to-end tests built on [Autto Framework](https://github.com/AnderCMD/Autto-Framework).

```bash
cp .env.example .env                       # local secrets, never committed
mvn test                                   # or ./mvnw if you add the Maven wrapper
mvn test -Dcucumber.filter.tags=@smoke -Dautto.browser.name=firefox
```

Open `target/autto-reports/index.html`. Configuration reference: the `docs/` folder of Autto.
