(function() {
// D3.layout.force3d.js
// (C) 2012 ziggy.jonsson.nyc@gmail.com
// BSD license (http://opensource.org/licenses/BSD-3-Clause)

    d3.layout.force3d = function() {
        var  forceXY = d3.layout.force()
            ,forceZ = d3.layout.force()
            ,zNodes = {}
            ,zLinks = {}
            ,nodeID = 1
            ,linkID = 1
            ,tickFunction = Object

        var force3d = {}

        Object.keys(forceXY).forEach(function(d) {
            force3d[d] = function() {
                var result = forceXY[d].apply(this,arguments)
                if (d !="nodes" && d!="links")  forceZ[d].apply(this,arguments)
                return (result == forceXY) ? force3d : result
            }
        })


        force3d.on = function(name,fn) {
            if(name == "tick"){
                tickFunction = fn;
            }
            else{
                forceXY.on(name, fn);                
            }
            return force3d
        }


        forceXY.on("tick",function() {

            // Refresh zNodes add new, delete removed
            var _zNodes = {}
            forceXY.nodes().forEach(function(d,i) {
                if (!d.id) d.id = nodeID++
                _zNodes[d.id] = zNodes[d.id] ||  {x:d.z,px:d.z,py:d.z,y:d.z,id:d.id}
                d.z =  _zNodes[d.id].x
            })
            zNodes = _zNodes

            // Refresh zLinks add new, delete removed
            var _zLinks = {}
            forceXY.links().forEach(function(d) {
                var nytt = false
                if (!d.linkID) { d.linkID = linkID++;nytt=true}
                _zLinks[d.linkID] = zLinks[d.linkID]  || {target:zNodes[d.target.id],source:zNodes[d.source.id]}

            })
            zLinks = _zLinks

            // Update the nodes/links in forceZ
            forceZ.nodes(d3.values(zNodes))
            forceZ.links(d3.values(zLinks))
            forceZ.start() // Need to kick forceZ so we don't lose the update mechanism

            // And run the user defined function, if defined
            if(tickFunction) tickFunction();
        })

        // Expose the sub-forces for debugging purposes
        force3d.xy = forceXY
        force3d.z = forceZ

        return force3d
    }
})()