## Drilling across

Drilling across or Drill across refers to a reporting feature that let you querying two or more fact tables.
Aggregated results are then stitch together on shared columns. There are several strategies to *stitch*
the results. Let's see various examples to learn how SquashQL does it.  

### Examples

#### Schema

Information about shipments and returns is captured in two tables: shipment and return. 

`shipment` captures the quantity of products sold:
```
+---------+----------+
| product | quantity |
+---------+----------+
|       A |       15 |
|       B |       23 |
|       C |       16 |
+---------+----------+
```

`return` captures the quantity of products returned:
```
+---------+----------+-----------+
| product | quantity |    reason |
+---------+----------+-----------+
|       A |        1 | defective |
|       C |        3 |  unwanted |
+---------+----------+-----------+
```

#### Case 1: results have all columns in common

Now let's ask to group by on product with a rollup to have a summary of each table:

```typescript
const query = from("shipment")
        .select(["product"], [], [sum("quantity sold", "quantity")])
        .rollup(["product"])
        .build()
```

Result:
```
+-------------+---------------+
|     product | quantity sold |
+-------------+---------------+
| Grand Total |            54 |
|           A |            15 |
|           B |            23 |
|           C |            16 |
+-------------+---------------+
```

A similar query can be executed on `return` table.
```typescript
const query = from("return")
        .select(["product"], [], [sum("quantity returned", "quantity")])
        .rollup(["product"])
        .build()
```

Result:
```
+-------------+-------------------+
|     product | quantity returned |
+-------------+-------------------+
| Grand Total |                 4 |
|           A |                 1 |
|           C |                 3 |
+-------------+-------------------+
```

To see both aggregate values quantity returned and quantity sold in the same table, we can use drilling across feature
of SquashQL:

```typescript
const queryShipment = from("shipment")
        .select(["product"], [], [sum("quantity sold", "quantity")])
        .rollup(["product"])
        .build()
const queryReturn = from("return")
        .select(["product"], [], [sum("quantity returned", "quantity")])
        .rollup(["product"])
        .build()

querier.executeQueryMerge(new QueryMerge(queryShipment, queryReturn))
        .then(result => console.log(result))
```

The results will be *merged* or *stitched* together on their common columns that are not aggregated values. In this case: product. 
Missing values will be filled with `null`. In `return` table, there is no entry for product `B` so the quantity returned value is 
set to `null`.

```
+-------------+---------------+-------------------+
|     product | quantity sold | quantity returned |
+-------------+---------------+-------------------+
| Grand Total |            54 |                 4 |
|           A |            15 |                 1 |
|           B |            23 |              null |
|           C |            16 |                 3 |
+-------------+---------------+-------------------+
```

#### Case 2: results have not all columns in common

Let's change the second query and add `reason` to the select.

```typescript
const queryReturnWithReason = from("return")
        .select(["product", "reason"], [], [sum("quantity returned", "quantity")])
        .rollup(["product", "reason"])
        .build()
```

When executed this query individually, the result looks like:
```
+-------------+-------------+-------------------+
|     product |      reason | quantity returned |
+-------------+-------------+-------------------+
| Grand Total | Grand Total |                 4 |
|           A |       Total |                 1 |
|           A |   defective |                 1 |
|           C |       Total |                 3 |
|           C |    unwanted |                 3 |
+-------------+-------------+-------------------+
```

```typescript
const queryShipment = from("shipment")
        .select(["product"], [], [sum("quantity sold", "quantity")])
        .rollup(["product"])
        .build()
const queryReturnWithReason = from("return")
        .select(["product", "reason"], [], [sum("quantity returned", "quantity")])
        .rollup(["product", "reason"])
        .build()

querier.executeQueryMerge(new QueryMerge(queryShipment, queryReturnWithReason))
        .then(result => console.log(result))
```

Only one column over two is common between the aggregated results: `product`. The second result contains `reason` which 
does not exist in the first result. 

SquashQL will fill aggregated values with `null` for entries that are 
not compatible with the result they come from. In our case, these entries are `["A", "defective"]` and `["C", "unwanted"]` for quantity sold. 
They do not exist in the first result because the column reason is not present so the values of quantity sold in the table for these entries are set to `null`. 

For other entries, the values of `reason` is either `Total` or `Grand Total` and in that case reason values are ignored and 
values for quantity sold are simply copy from the first result.

```
+-------------+-------------+---------------+-------------------+
|     product |      reason | quantity sold | quantity returned |
+-------------+-------------+---------------+-------------------+
| Grand Total | Grand Total |            54 |                 4 |
|           A |       Total |            15 |                 1 |
|           A |   defective |          null |                 1 |
|           B |       Total |            23 |              null |
|           C |       Total |            16 |                 3 |
|           C |    unwanted |          null |                 3 |
+-------------+-------------+---------------+-------------------+
```