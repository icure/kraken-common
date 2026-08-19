function (keys, values, rereduce) {
	/*
	 * The fingerprints of the keypairs usable with every exchange data seen so far. An emitted value and a reduced
	 * value have the same shape, so reduce and rereduce are the same operation, and the result can only shrink.
	 *
	 * This is deliberately the naive scan. Seeding with the shortest list, hashing one side into a Set, or emitting
	 * sorted lists and merging them were all measured against it on a database shaped like a large one: the merge is
	 * ~12x faster in isolation on the spidermonkey couchdb 3.4 embeds, and none of them made the index build any
	 * faster, because what the reduce costs is the round trip to the query server rather than the javascript inside
	 * it. Hashing is in fact slower on the shape that dominates, lists of ten fingerprints. See adr 0007.
	 */
	var result = values[0]
	for (var i = 1; i < values.length && result.length > 0; i++) {
		/*
		 * Scan the shorter of the two lists and look its entries up in the longer one, whichever way round they
		 * arrive. The work is the same product either way, but indexOf stops at a match, and a match therefore saves
		 * half of whichever list is being searched - which is a bigger saving when that is the longer one.
		 *
		 * The result then comes out in the order of whichever list was shorter. Nothing depends on that: the value is
		 * read as a set.
		 */
		var shorter = result
		var longer = values[i]
		if (shorter.length > longer.length) {
			shorter = longer
			longer = result
		}
		var intersection = []
		for (var j = 0; j < shorter.length; j++) {
			if (longer.indexOf(shorter[j]) !== -1) {
				intersection.push(shorter[j])
			}
		}
		// The intersection only shrinks, so once it is empty nothing later can put a fingerprint back. That is a
		// common outcome rather than an edge case: an exchange data over the cap of the map contributes an empty
		// list, and so does a counterpart whose exchange data share no keypair at all.
		if (intersection.length === 0) return intersection
		result = intersection
	}
	return result
}
