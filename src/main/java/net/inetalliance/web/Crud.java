package net.inetalliance.web;

import net.inetalliance.potion.Locator;
import net.inetalliance.potion.info.Info;
import net.inetalliance.potion.info.Property;
import net.inetalliance.potion.query.Autocomplete;
import net.inetalliance.potion.query.Search;
import net.inetalliance.types.json.Json;
import net.inetalliance.types.json.JsonList;
import net.inetalliance.types.json.JsonMap;
import net.inetalliance.util.security.auth.Authorized;
import net.inetalliance.validation.ValidationErrors;
import net.inetalliance.validation.Validator;
import net.inetalliance.web.errors.InternalServerError;
import net.inetalliance.web.errors.MethodNotAllowedError;
import net.inetalliance.web.errors.NotFoundError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.inetalliance.funky.ClassFun.*;
import static net.inetalliance.funky.StringFun.*;
import static net.inetalliance.potion.Locator.*;
import static net.inetalliance.types.util.LocalizedMessages.*;

public class Crud<O>
		extends JsonProcessor {
	public static final String queryParameter = "query";
	public static final String queryLimitParameter = "limit";
	public static final String pageStartParameter = "start";
	public static final Json emptyList = Crud.toJson(JsonList.empty);

	public Crud() {
	}

	protected static Predicate<? super Property> getPropertyFilter(final HttpServletRequest request) {
		final String[] strings = request.getParameterValues("property");
		final List<String> properties = strings == null ? null : Arrays.asList(strings);
		if (properties == null || properties.isEmpty()) {
			return p -> true;
		} else {
			return p -> properties.contains(p.getName());
		}
	}

	protected Json respond(final HttpServletRequest request, final Info<O> info, final Collection<O> o) {
		return toJson(request, info, o);
	}

	protected Json respond(final HttpServletRequest request, final Info<O> info, final O o) {
		if (o == null) {
			return toJson(false, $M(request.getLocale(), "notFound"), null, null);
		} else {
			return toJson(true, null, 1, JsonList.singleton(toJson(request, info, o)));
		}
	}

	@SuppressWarnings({"unchecked"})
	protected Info<O> getInfo(final HttpServletRequest request) {
		return Info.$((Class<O>) Locator.types.get(request.getParameter("type")));
	}

	@SuppressWarnings({"unchecked"})
	protected Info<O> getInfo(final JsonMap data) {
		return Info.$((Class<O>) Locator.types.get(data.get("type")));
	}

	protected Object getKey(final HttpServletRequest request) {
		return request.getParameter("key");
	}

	protected Object getKey(final JsonMap data) {
		return data.getInteger("id");
	}

	protected static Json respond(final ValidationErrors errors) {
		final JsonMap json = new JsonMap();
		json.put("success", false);
		json.put("errors", errors.toJsonMap());
		return json;
	}

	public static <T> Json toJson(final HttpServletRequest request, final Info<T> info, final Collection<T> objects) {
		final JsonList list = new JsonList(objects.size());
		for (final T t : objects) {
			list.add(toJson(request, info, t));
		}
		return toJson(list);
	}

	private static <T> Json toJson(final HttpServletRequest request, final Info<T> info, final T object) {
		Locator.read(object);
		return info.toJson(object, getPropertyFilter(request));
	}

	public static Json toJson(final JsonList data) {
		return toJson(true, null, data.size(), data);
	}

	public static <C extends Collection & Json> Json toJson(final C data) {
		return toJson(true, null, data.size(), data);
	}

	public static Json toJson(final Json data, final int size) {
		return toJson(true, null, size, data);
	}

	public static <O> Json toJson(final Collection<O> objects, final Function<O, JsonMap> functor) {
		final JsonList data = objects.stream().map(functor).collect(Collectors.toCollection(JsonList::new));
		return toJson(true, null, objects.size(), data);
	}

	public static Json toJson(final boolean success, final String message, final Integer total, final Json data) {
		final JsonMap json = new JsonMap();
		json.put("success", success);
		if (message != null) {
			json.put("message", message);
		}
		if (total != null) {
			json.put("total", total);
		}
		if (data != null) {
			json.put("data", data);
		}
		return json;
	}

	@Override
	public Json $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response,
			final Authorized authorized)
			throws Throwable {
		switch (method) {
			case POST:
				return create(request, authorized, JsonMap.parse(request.getInputStream()));
			case GET:
				return read(request);
			case PUT:
				return update(request, authorized, JsonMap.parse(request.getInputStream()));
			case DELETE:
				return delete(request, authorized, JsonMap.parse(request.getInputStream()));
			default:
				throw new MethodNotAllowedError();
		}
	}

	protected Json delete(final HttpServletRequest request, final Authorized authorized, final JsonMap data)
			throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		final Info<O> info = getInfo(request);
		final O object = getObject(info, request, data.getInteger("data"));
		if (object == null) {
			throw new NotFoundError();
		} else {
			Locator.delete(authorized.getName(), object);
			return toJson(true, null, 0, JsonList.empty);
		}
	}

	protected Json update(final HttpServletRequest request, final Authorized authorized, final JsonMap data)
			throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		final Info<O> info = getInfo(data);
		final JsonMap values = data.getMap("data");
		final O canonical = getObject(info, request, data);
		Locator.read(canonical);

		final ValidationErrors errors = Locator.update(canonical, authorized.getName(), copy -> {
			for (final Property<O, ?> property : info.properties) {
				if (property.containsProperty(values)) {
					property.setIf(copy, values);
				}
			}
			return Validator.update(request.getLocale(), copy);
		});

		if (errors.isEmpty()) {
			return respond(request, info, canonical);
		} else {
			return respond(errors);
		}

	}

	protected Json create(final HttpServletRequest request, final Authorized authorized, final JsonMap data)
			throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		final Info<O> info = getInfo(request);
		final O object = info.type.getDeclaredConstructor().newInstance();
		info.properties().filter(p -> !p.isGenerated()).forEach(property -> {
			property.setIf(object, data);
		});
		final ValidationErrors errors = Validator.create(request.getLocale(), object);
		if (errors.isEmpty()) {
			Locator.create(authorized.getName(), object);
			return respond(request, info, object);
		} else {
			return respond(errors);
		}
	}

	protected Json read(final HttpServletRequest request)
			throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		final Info<O> info = getInfo(request);
		final String query = request.getParameter(queryParameter);
		if (isEmpty(query)) {
			final Object key = getKey(request);
			if (key != null) {
				return respond(request, info, getObject(info, request, key));
			} else {
				return respond(request, info, getObjects(info, request));
			}
		} else {
			// autocomplete
			return respond(request, info, autocomplete(info, request));
		}
	}

	protected List<O> autocomplete(final Info<O> info, final HttpServletRequest request) {
		final String query = request.getParameter(queryParameter);
		if (query == null) {
			return Collections.emptyList();
		}
		final Integer limit = getParam(request, queryLimitParameter, s -> s.map(Integer::valueOf).orElse(null));
		final Autocomplete<O> autocomplete = new Autocomplete<>(info.type, limit, space.split(query));
		final List<O> results = new ArrayList<>(limit);
		results.addAll($$(autocomplete));
		results.sort(autocomplete);
		return results;
	}

	protected Set<O> getObjects(final Info<O> info, final HttpServletRequest request) {
		final String query = request.getParameter(queryParameter);
		return query == null ? $A(info.type) : $$(new Search<>(info.type, space.split(query)));
	}

	protected O getObject(final Info<O> info, final HttpServletRequest request, final JsonMap data)
			throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		return getObject(info, request, getKey(data.getMap("data")));
	}

	protected O getObject(final Info<O> info, final HttpServletRequest request, final Object key)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		final O t = info.type.getDeclaredConstructor().newInstance();
		set(t, info.keys().findFirst().orElseThrow(), key);
		return Locator.$(t);
	}

	protected <P> void set(final O t, final Property<O, P> property, final Object value) {
		try {
			property.field.set(t, convert(property.type, value));
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	protected static final Pattern space = Pattern.compile("[ ]");

}
