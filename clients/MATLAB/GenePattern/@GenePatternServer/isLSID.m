function a = isLSID(obj, nameOrId)

	idx = findstr('urn:lsid:', nameOrId);

	a =  not (isempty (idx));


