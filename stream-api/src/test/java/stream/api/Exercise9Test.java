package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.CollectorImpl;
import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy
    @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<StringBuilder> supplier = () -> new StringBuilder();
        BiConsumer<StringBuilder, String> accumulator = (stringBuilder, s) -> stringBuilder.append(s).append(",");
        BinaryOperator<StringBuilder> combiner = (sb, sb2) -> new StringBuilder(sb.toString() + sb2.toString());
        Function<StringBuilder, String> finisher = o -> o.deleteCharAt(o.length() - 1).toString();


        Collector<String, ?, String> toCsv =
                new CollectorImpl(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Difficult
    @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String, Set<String>>> supplier = () -> new HashMap<>();
        BiConsumer<Map<String, Set<String>>, Customer> accumulator = (map, customer) -> customer.getWantToBuy().forEach(
                item -> map.merge(item.getName(), new HashSet<String>() {{
                    add(customer.getName());
                }}, (set, set2) -> {
                    set.addAll(set2);
                    return set;
                }));

        BinaryOperator<Map<String, Set<String>>> combiner = (Map<String, Set<String>> map, Map<String, Set<String>> map2) -> {
            map2.forEach((key, set) ->
                    map.merge(key, set, (setLeft, setRight) -> {
                        setLeft.addAll(setRight);
                        return setLeft;
                    }));
            return map;
        };
        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = stringSetHashMap -> stringSetHashMap;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
                new CollectorImpl(supplier, accumulator, combiner, finisher, EnumSet.of(
                        Collector.Characteristics.CONCURRENT,
                        Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }

    @Difficult
    @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */
        Supplier<StringBuilder> supplier = () -> new StringBuilder();
        BiConsumer<StringBuilder, String> accumulator = (stringBuilder, s) -> {
            String[] digits = s.split("-");
            if (digits.length > 1) {
                IntStream.rangeClosed(Integer.valueOf(digits[0]), Integer.valueOf(digits[1])).forEach(i -> stringBuilder.append(i).append(","));
            } else {
                stringBuilder.append(digits[0]).append(",");
            }
        };
        BinaryOperator<StringBuilder> combiner = (sb, sb2) -> new StringBuilder(sb.toString() + sb2.toString());
        Function<StringBuilder, String> finisher = o -> {
            String[] digitsArr = o.deleteCharAt(o.length() - 1).toString().split(",");
            Integer max = Stream.of(digitsArr).map(Integer::valueOf).max(Integer::compare).get();
            List<Integer> digits = Stream.of(digitsArr).map(Integer::valueOf).collect(Collectors.toList());
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < max; i++) {
                result.append(digits.contains(i + 1) ? "1" : "0");
            }
            return result.toString();
        };


        Collector<String, ?, String> toBitString = Collector.of(supplier, accumulator, combiner, finisher);

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }

}