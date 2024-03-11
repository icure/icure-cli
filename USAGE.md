# iCure CLI

The icure cli is a command line interface for the icure project.

It is provided as a tarball or zip file and can be installed by extracting it and running the `bin/icurecli` script.

Without extra arguments, the CLI will launch an interactive shell but arguments can be added to execute commands directly.

```bash
❯ ./bin/icurecli
IQR > api -h
Usage: api [<options>] <command> [<args>]...

Options:
  -u, --credentials=<text>  Credentials
  -s, --server=<text>       Couchdb server URL
  -h, --help                Show this message and exit

Commands:
  deploy-codes

IQR > api deploy-codes -h
missing option --credentials

IQR > api -u a:a deploy-codes -h
Usage: api deploy-codes [<options>] [<path>]

Options:
  -h, --help  Show this message and exit

Arguments:
  <path>  The path of the codes file to import. The codes file should be a JSON file with an array of objects with the following fields: id, code, type, version, regions, labels.

IQR > 
```

## Commands

`api` is the only public command available at the moment. It proposes a `deploy-codes` command to import codes into icure databases.

```bash
./bin/icurecli api -u admin@icure.cloud:****** deploy-codes < /tmp/test.json
```

In this example /tmp/test.json is a file containing a JSON array of objects with the following fields: id, code, type, version, regions, labels:

```json
[
  {
    "id": "BE-COUNTRY|205.0|1",
    "regions": [
      "fr",
      "be"
    ],
    "type": "BE-COUNTRY",
    "code": "205.0",
    "version": "1",
    "label": {
      "fr": "République de Singapour",
      "nl": "Republiek Singapore",
      "en": "Republic of Singapore"
    }
  },
  {
    "id": "BE-COUNTRY|151.0|1",
    "regions": [
      "fr",
      "be"
    ],
    "type": "BE-COUNTRY",
    "code": "151.0",
    "version": "1",
    "label": {
      "fr": "Monténégro",
      "nl": "Montenegro",
      "en": "Montenegro"
    }
  }
]
```



