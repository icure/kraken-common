function (keys, values, rereduce) {
	// The fingerprints of the keypairs usable with every exchange data seen so far. An emitted value and a reduced
	// value have the same shape, so reduce and rereduce are the same operation, and the result can only shrink.
	var result = values[0]
	for (var i = 1; i < values.length && result.length > 0; i++) {
		var other = values[i]
		var intersection = []
		for (var j = 0; j < result.length; j++) {
			if (other.indexOf(result[j]) !== -1) {
				intersection.push(result[j])
			}
		}
		result = intersection
	}
	return result
}
