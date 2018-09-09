# brick-store

Project for learning pure functional programming in Scala with the Typelevel ecosystem.

## Development

1. Run postgres (localhost, db: postgres, user: postgres, no password) - to change the config,
see `application.conf` in this project.

You can use docker for this:

`$ docker run -p 5432:5432 postgres:10.2`

Or the local postgres server. If you installed it with `brew`, do `createuser postgres`
and everything else should already be there (assuming the brew service is running).

2. Run the application, e.g. by going into `sbt` and typing `run`.

To check that your DB config is right, look for errors in the log (most likely errors from Flyway).
If there are none, you should be good. You can also query the `GET /bricks` endpoint.

## What can I do here?

Brick Store offers a few HTTP resources with some endpoints:

Bricks:

- `GET /bricks` - lists the existing bricks. 
- `POST /bricks/import` - takes a stream of `org.typelevel.brickstore.dto.BrickToCreate` objects
(see `input-data.jsonl` file for an example of the format), returns an `org.typelevel.brickstore.dto.ImportResult`
for each line (for successes - the brick's ID, for failures - a `cats.data.NonEmptyList` of errors).

Cart:
- `POST /cart/add` - takes a `org.typelevel.brickstore.dto.CartAddRequest` and adds a brick to the cart.
If any errors occur, a NEL of values of type `org.typelevel.brickstore.dto.CartAddError` will be returned.
- `GET /cart` - gets the current user's cart (the user is hardcoded to id=1, so everyone is that user ;).
The result is a non-empty list of `org.typelevel.brickstore.dto.CartBrick`, or 404 if there isn't anything in the cart.

Orders:
- `POST /order` - creates an order, if the user has a non-empty cart. Fails otherwise.
- `GET /order/stream` - streams existing and incoming order summaries (id, user id, total price). More below.  

If you open the `GET /order/stream` endpoint and let it run, you'll see a new item for every order
created after you call it. For best results, you can use the `httpie` CLI tool to convert the streamed lines of JSON
to a pretty format:

```$ http :8080/order/stream --stream```

To see how the application works in full, you can run the following flow:

## Demo

Samples are using httpie.

### Create some bricks

```$ http post :8080/bricks/import < src/main/resources/input-data.jsonl```

You should see something like this:

```json
HTTP/1.1 202 Accepted
Content-Type: application/json
Date: Sun, 09 Sep 2018 21:44:37 GMT
Transfer-Encoding: chunked

{"result":1}
{"result":2}
{"result":3}
{"result":4}
{"result":5}
{"lineNumber":6,"error":["NameTooLong"]}
{"result":6}
...
{"result":40}
{"result":41}
```

Which means 41 results were added, but the one in line 6 wasn't (rightfully, because its name is longer than
the maximum allowed length, see `org.typelevel.brickstore.BricksServiceImpl#validate`).

Having added bricks, we can add them to the cart so that we can order them.

### Adding bricks to the cart

```$ http post :8080/cart/add brickId=3 quantity=2```
```$ http post :8080/cart/add brickId=38 quantity=800```

If all goes well, these should both return `200 OK`.

Try adding a brick that doesn't exist or passing a non-positive quantity.

### Listing orders

To see the existing orders and subscribe to future ones (until you drop the connection):

```$ http :8080/order/stream --stream```

It'll timeout in 30 seconds, so be quick (or call it again when it does)!

**Note:** You can have as many clients connecting to this endpoint as you wish, and they should all see consistent output.

### Create an order

Assuming your cart isn't empty, you should be able to create an order:

```json
$ http post :8080/order
HTTP/1.1 201 Created
Content-Length: 24
Content-Type: text/plain; charset=UTF-8
Date: Sun, 09 Sep 2018 21:52:56 GMT

Created order with id: 1
```

And, if the order stream is opened, you'll see an output similar to this:

```json
$ http :8080/order/stream --stream
HTTP/1.1 200 OK
Content-Type: application/json
Date: Sun, 09 Sep 2018 21:52:55 GMT
Transfer-Encoding: chunked

{
   "id": 1,
   "total": 705260,
   "userId": 1
}
```

If you call this endpoint again, you should see the order as well.
