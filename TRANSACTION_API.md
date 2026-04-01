# Transaction Controller API

## Endpoints

### GET /transaction/my-transactions
Returns all transactions where the authenticated user is the owner, with optional status filtering.

### GET /transaction/renter
Returns all transactions where the authenticated user is the renter (their rental requests).

### Authentication
- **Required**: JWT token in Authorization header
- **Format**: `Bearer <jwt-token>`

### Query Parameters (for /my-transactions)
- `status` (optional): Filter transactions by status
  - Values: `PENDING`, `APPROVED`, `ACTIVE`, `COMPLETED`, `CANCELLED`

### Response
Both endpoints return a list of `TransactionResponseDto` objects containing:

```json
[
  {
    "transactionID": 1,
    "ownerName": "John Doe",
    "ownerEmail": "john@example.com",
    "renterName": "Jane Smith",
    "renterEmail": "jane@example.com",
    "itemName": "Mountain Bike",
    "itemDescription": "High-quality mountain bike",
    "transactionType": "RENT",
    "startDate": "2026-04-01",
    "endDate": "2026-04-05",
    "requestedDate": "2026-03-30T10:00:00",
    "approvedDate": "2026-03-31T14:30:00",
    "returnedDate": null,
    "transactionStatus": "ACTIVE",
    "dailyRate": 25.00,
    "depositAmount": 100.00,
    "totalDays": 4,
    "totalAmount": 100.00,
    "paymentStatus": "PAID",
    "paymentRef": "TNG-123456",
    "paymentDate": "2026-03-31T15:00:00",
    "renterNote": "Please deliver to my office",
    "ownerNote": "Item in excellent condition"
  }
]
```

### Example Usage

#### Owner: Get all my transactions
```bash
GET /transaction/my-transactions
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Owner: Get only active transactions
```bash
GET /transaction/my-transactions?status=ACTIVE
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Renter: Get my rental requests
```bash
GET /transaction/renter
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Error Responses

#### Missing Authorization Header
```json
{
  "error": "Authorization token required"
}
```

#### Invalid Token
```json
{
  "error": "Invalid token"
}
```

### Implementation Notes

- User ID is extracted from the JWT token's `userId` claim
- Status filtering is case-insensitive
- Response includes all transaction details with resolved names (not just IDs)
- For performance, consider adding a database-level filter for status in production
