package it.bancaditalia.oss.sdmx.util;

import java.util.Iterator;

import it.bancaditalia.oss.sdmx.api.BaseObservation;
import it.bancaditalia.oss.sdmx.api.DoubleObservation;
import it.bancaditalia.oss.sdmx.api.PortableTimeSeries;

/**
 * Various convenience methods and interfaces.
 * 
 * @author Valentino Pinna
 *
 */
public class Utils
{
	public static <T> Function<BaseObservation<? extends T>, String> timeslotExtractor()
	{
		return new Function<BaseObservation<? extends T>, String>() {
				@Override
				public String apply(BaseObservation<? extends T> obs)
				{
					return obs.getTimeslot();
				}
			};
	}

	public static <T> Function<BaseObservation<? extends T>, T> obsExtractor()
	{
		return new Function<BaseObservation<? extends T>, T>() {
			@Override
			public T apply(BaseObservation<? extends T> obs)
			{
				return obs.getValue();
			}
		};
	}

	public static <T> Function<BaseObservation<? extends T>, String> obsLevelAttrsExtractor(final String attributeName)
	{
		return new Function<BaseObservation<? extends T>, String>() {
			@Override
			public String apply(BaseObservation<? extends T> obs)
			{
				return obs.getAttributeValue(attributeName);
			}
		};
	}

	private Utils()
	{
	}

	public static interface Function<T, R>
	{
		public R apply(T value);
	};

	public static interface BiFunction<T, U, R>
	{
		public R apply(T left, U right);
	};

	public static interface BinaryOperator<T> extends BiFunction<T, T, T>
	{
	};

	public static interface DoubleUnaryOperator
	{
		public double applyAsDouble(double argument);
	};

	public static interface DoubleBinaryOperator
	{
		public double applyAsDouble(double left, double right);
	};

	public static <T> Function<T, T> identity()
	{
		return new Function<T, T>() {
			@Override
			public T apply(T obs)
			{
				return obs;
			}
		};
	}

	/**
	 * Extract values from a series and returns an array of primitive type {@code double}. It uses
	 * {@link BaseObservation#getValueAsDouble()} to perform conversion.
	 * 
	 * @param series The series to convert. It must not be null.
	 * @return The converted series.
	 */
	public static double[] toDoubleArray(PortableTimeSeries<?> series)
	{
		double result[] = new double[series.size()];
		for (int i = 0; i < series.size(); i++)
			result[i] = series.get(i).getValueAsDouble();
		return result;
	}

	/**
	 * Convert a series into an optimized series for {@code double} values. It uses
	 * {@link BaseObservation#getValueAsDouble()} to perform conversion.
	 * 
	 * @param series The series to convert. It must not be null.
	 * @return The converted series.
	 */
	public static PortableTimeSeries<Double> mapToDoubleValues(PortableTimeSeries<?> series)
	{
		PortableTimeSeries<Double> result = new PortableTimeSeries<>(series);

		for (BaseObservation<?> obs : series)
			result.add(new DoubleObservation(obs, obs.getValueAsDouble()));

		return result;
	}

	/**
	 * Creates a new PortableTimeSeries over the same time slots of both series with each of its values being set equal
	 * to the result of respectively applying a function over each corresponding pair of values of left and right
	 * {@code PortableTimeSeries<Double>}.
	 * 
	 * @param left The left operand series to combine
	 * @param right The right operand series to combine
	 * @param combiner The {@link DoubleBinaryOperator} that combines two values.
	 * @return The new PortableTimeSeries
	 * 
	 * @throws UnsupportedOperationException if the two series do not have the same size.
	 * @throws NullPointerException if either of the three parameters is null.
	 */
	public static PortableTimeSeries<Double> combineValuesAsDouble(PortableTimeSeries<Double> left,
			PortableTimeSeries<Double> right, DoubleBinaryOperator combiner)
	{
		if (left.size() != right.size())
			throw new UnsupportedOperationException("The two series do not have the same size.");

		PortableTimeSeries<Double> newSeries = new PortableTimeSeries<>(left);
		Iterator<BaseObservation<? extends Double>> rightIterator = right.iterator();

		for (BaseObservation<? extends Double> obs : left)
			newSeries.add(new DoubleObservation(obs,
					combiner.applyAsDouble(obs.getValueAsDouble(), rightIterator.next().getValueAsDouble())));

		return newSeries;
	}

	/**
	 * Creates a new PortableTimeSeries over the same time slots of a {@code PortableTimeSeries<Double>} with each of
	 * its values being set equal to the result of respectively applying a function over each of the series' values.
	 * 
	 * @param mapper The function that maps values.
	 * @return The new PortableTimeSeries
	 * 
	 * @throws NullPointerException if mapper is null.
	 */
	public static PortableTimeSeries<Double> mapValuesAsDouble(PortableTimeSeries<Double> series,
			DoubleUnaryOperator mapper)
	{
		PortableTimeSeries<Double> newSeries = new PortableTimeSeries<>(series);

		for (BaseObservation<? extends Double> obs : series)
			newSeries.add(new DoubleObservation(obs, mapper.applyAsDouble(obs.getValueAsDouble())));

		return newSeries;
	}
}
