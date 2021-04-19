# qbo

```
{
    "Invoice": {
        "AllowIPNPayment": false,
        "AllowOnlinePayment": false,
        "AllowOnlineCreditCardPayment": false,
        "AllowOnlineACHPayment": false,
        "domain": "QBO",
        "sparse": false,
        "Id": "149",
        "SyncToken": "1",
        "MetaData": {
            "CreateTime": "2021-04-19T06:39:58-07:00",
            "LastUpdatedTime": "2021-04-19T06:42:29-07:00"
        },
        "CustomField": [
            {
                "DefinitionId": "1",
                "Name": "Crew #",
                "Type": "StringType"
            },
            {
                "DefinitionId": "2",
                "Name": "startDate",
                "Type": "StringType",
                "StringValue": "2021-09-09T11:11:11"
            },
            {
                "DefinitionId": "3",
                "Name": "endDate",
                "Type": "StringType",
                "StringValue": "2021-10-10T11:11:11"
            }
        ],
        "DocNumber": "1042",
        "TxnDate": "2021-04-19",
        "CurrencyRef": {
            "value": "USD",
            "name": "United States Dollar"
        },
        "LinkedTxn": [],
        "Line": [
            {
                "Id": "1",
                "LineNum": 1,
                "Amount": 100.00,
                "DetailType": "SalesItemLineDetail",
                "SalesItemLineDetail": {
                    "ItemRef": {
                        "value": "1",
                        "name": "Services"
                    },
                    "TaxCodeRef": {
                        "value": "NON"
                    }
                }
            },
            {
                "Amount": 100.00,
                "DetailType": "SubTotalLineDetail",
                "SubTotalLineDetail": {}
            }
        ],
        "TxnTaxDetail": {
            "TotalTax": 0
        },
        "CustomerRef": {
            "value": "1",
            "name": "Amy's Bird Sanctuary"
        },
        "BillAddr": {
            "Id": "2",
            "Line1": "4581 Finch St.",
            "City": "Bayshore",
            "CountrySubDivisionCode": "CA",
            "PostalCode": "94326",
            "Lat": "INVALID",
            "Long": "INVALID"
        },
        "ShipAddr": {
            "Id": "2",
            "Line1": "4581 Finch St.",
            "City": "Bayshore",
            "CountrySubDivisionCode": "CA",
            "PostalCode": "94326",
            "Lat": "INVALID",
            "Long": "INVALID"
        },
        "DueDate": "2021-05-19",
        "TotalAmt": 100.00,
        "ApplyTaxAfterDiscount": false,
        "PrintStatus": "NeedToPrint",
        "EmailStatus": "NotSet",
        "Balance": 100.00
    },
    "time": "2021-04-19T09:28:56.878-07:00"
}
```
