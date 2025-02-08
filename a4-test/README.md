## Testing

Before running `gradle build jacocoTestReport`, bring up a few test servers with the supplied Docker compose file in this project.

> Note: the docker compose dependent tests can run on MacOS, but the networking setup is too cumbersome. So just run tests inside a Linux machine, much simpler.

Generate a self-signed SSL certificate for local subdomains:

```
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -addext "subjectAltName = DNS:*.localhost" \
  -keyout ./awe4lb.key -out ./awe4lb.pem
```

## Security checks

1. Generate UDP connections in large numbers. This should exercise the LB's capability to clear out expired UDP sessions (TTL timeout).

## Resources

Test audio:

- https://archive.org/details/MusopenCollectionAsFlac/Bach_GoldbergVariations/JohannSebastianBach-16-GoldbergVariationsBwv.988-Variation15.CanonOnTheFifth.flac
- https://ia800307.us.archive.org/34/items/MusopenCollectionAsFlac/Bach_GoldbergVariations/JohannSebastianBach-16-GoldbergVariationsBwv.988-Variation15.CanonOnTheFifth.flac
