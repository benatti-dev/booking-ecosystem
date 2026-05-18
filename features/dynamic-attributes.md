# Dynamic Service Attributes

## Problem

Different business types have fundamentally different service attributes:
- Beauty salon: `hair_length`, `coloring`, `treatment_type`
- Tennis court: `court_surface`, `indoor`, `racket_rental`
- Medical clinic: `specialty`, `appointment_type`, `insurance_accepted`

Hard-coding these fields requires code changes whenever a new business type is added.

## Solution: Schema-Driven JSONB

### Attribute Definition Schema

```sql
service_attribute_definitions:
  category_id  → linked to business type
  field_key    → 'hair_length'
  field_label  → 'Hair Length'
  field_type   → TEXT | NUMBER | SELECT | BOOLEAN | MULTI_SELECT
  options      → ["short","medium","long"]  (for SELECT/MULTI_SELECT)
  is_required  → true/false
  sort_order   → display order
```

### Service Attribute Storage

```sql
services.attributes JSONB:
{
  "hair_length": "long",
  "coloring": true,
  "treatment_type": "keratin"
}
```

### Validation on Save

```java
ServiceAttributeValidator.validate(categoryId, attributes):
  1. Load definitions for categoryId (@Cacheable)
  2. For each definition with is_required=true → check presence in attributes
  3. Validate value type (string, boolean, number)
  4. For SELECT/MULTI_SELECT → check that value is from options list
  5. Return Map<String, String> of errors or empty map
```

### Caching

```java
@Cacheable(value = "attributeDefs", key = "#categoryId")
public List<ServiceAttributeDefinition> findByCategory(Long categoryId) { ... }

@CacheEvict(value = "attributeDefs", key = "#categoryId")
public void updateDefinition(Long categoryId, ...) { ... }
```

## Frontend — Dynamic Form

```typescript
// Load definitions for the business category
// Render fields dynamically:
switch (definition.fieldType) {
  case 'TEXT':         → <input type="text">
  case 'NUMBER':       → <input type="number">
  case 'SELECT':       → <mat-select> with options
  case 'BOOLEAN':      → <mat-checkbox>
  case 'MULTI_SELECT'  → <mat-select [multiple]="true">
}
```

## Zero-Code Extension

To add a new business type (e.g. "car wash"):
1. `POST /admin/categories` → `{ name: "car_wash", label: "Car Wash" }`
2. `POST /admin/categories/{id}/attribute-definitions` → define attributes
3. Business owners can immediately register and use the new attributes

**No code changes required.**
