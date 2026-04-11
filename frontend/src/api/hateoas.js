/**
 * Unpack Spring HATEOAS HAL collections: { _embedded: { someKey: [ items... ] }, _links: {...} }
 * or a plain array for backwards compatibility.
 */
export function unwrapHalCollection(payload) {
  if (payload == null) return []
  if (Array.isArray(payload)) return payload
  const emb = payload._embedded
  if (emb && typeof emb === 'object') {
    return Object.values(emb).flat().filter(Boolean)
  }
  return []
}
