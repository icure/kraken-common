function (keys, values, rereduce) {
    return values.reduce((a, b) => (a.m > b.m ? a : b));
}
