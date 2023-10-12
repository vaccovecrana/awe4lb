# awe4lb

`awe4lb` is a layer 4 load balancer

## UDP configuration

- Set the size of the reception buffer according to your application's requirements.

## Security considerations

- Do not allow public access to the REST api, since it allows for full management. Expose it only within a trusted network perimeter.
- Most applications and use cases should work fine with the default TCP buffer size. However, applications which stream large amounts of data should make sure that the underlying hardware has enough memory capacity to handle backpressure from either clients or backends.

## Testing

Generate a self-signed SSL certificate for local subdomains:

```
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -addext "subjectAltName = DNS:*.localhost" \
  -keyout ./awe4lb.key -out ./awe4lb.pem
```

## Implementation items

### TCP

- [ ] Thread-per-request connection tracking PoC. Non-SSL.
- [ ] Schema modeling (configuration template).
- [ ] SSL connection tracking.

### UDP

- [ ] UDP backend selection strategies (weight, random, round robin).
- [ ] Sticky session support.
- [ ] Transparent proxying?

### Backend discovery

- [ ] DNS records.
- [ ] Exec return value.
- [ ] Any others provided by `gobetween`.

### Monitoring

- [ ] Metrics capturing.
  - [ ] Bytes sent/received (global).
  - [ ] Whichever other metrics `gobetween` exposes.
- [ ] Metrics access (prometheus endpoint).

### Admin functionality

- [ ] REST API access for configuration changes.
- [ ] UI implementation.
- [ ] Documentation/Usage notes/caveats.


## Resources

Test audio:

- https://archive.org/details/MusopenCollectionAsFlac/Bach_GoldbergVariations/JohannSebastianBach-16-GoldbergVariationsBwv.988-Variation15.CanonOnTheFifth.flac
- https://ia800307.us.archive.org/34/items/MusopenCollectionAsFlac/Bach_GoldbergVariations/JohannSebastianBach-16-GoldbergVariationsBwv.988-Variation15.CanonOnTheFifth.flac

- https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names
- https://stackoverflow.com/questions/53323855/sslserversocket-and-certificate-setup
- https://stackoverflow.com/a/62263402/491160
- https://github.com/raell2/SSLAsynchronousSocketChannel/tree/master
- https://stackoverflow.com/questions/14225957/socket-vs-socketchannel
- https://bugs.openjdk.org/browse/JDK-8202625
- https://github.com/yyyar/gobetween/issues/335
