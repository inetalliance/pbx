package net.inetalliance.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import net.inetalliance.potion.info.Info;
import net.inetalliance.types.json.Json;
import net.inetalliance.types.json.JsonBoolean;
import net.inetalliance.types.json.JsonList;
import net.inetalliance.types.json.JsonMap;

import java.util.Collection;
import java.util.Set;

public abstract class Tree<T>
        extends Crud<T> {

    protected Tree() {
    }

    protected abstract Set<T> getChildren(final HttpServletRequest request, final String key);

    protected abstract boolean isLeaf(final T node);

    protected abstract JsonMap toJson(final T object);

    @Override
    protected Set<T> getObjects(final Info<T> info, final HttpServletRequest request) {
        return getChildren(request, request.getParameter("node"));
    }

    @Override
    protected Json respond(final HttpServletRequest request, final Info<T> tInfo,
                           final Collection<T> objects) {
        val list = new JsonList(objects.size());
        for (val t : objects) {
            val json = toJson(t);
            json.put("leaf", JsonBoolean.$(isLeaf(t)));
            list.add(json);
        }
        return toJson(true, null, list.size(), list);
    }

    @Override
    protected Json respond(final HttpServletRequest request, final Info<T> tInfo, final T t) {
        val jsonMap = toJson(t);
        jsonMap.put("leaf", JsonBoolean.$(isLeaf(t)));
        return toJson(true, null, 1, jsonMap);
    }
}


