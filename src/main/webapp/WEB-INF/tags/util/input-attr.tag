<%@ tag body-content="empty" dynamic-attributes="dynattrs" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 

<input 
	<c:forEach items="${dynattrs}" var="a"> 
		<c:if test="${not empty a.value}">
${a.key}="${a.value}" 
		</c:if>
	</c:forEach> 
>
<jsp:doBody/>
<!-- </input> -->
