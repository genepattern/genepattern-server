function testGetMethodParameters(obj)

am = getMethods(obj.server);
    
for i=1:length(am)
    meth = am(i);
   
    params = getMethodParameters(obj.server, meth.name);
        
end