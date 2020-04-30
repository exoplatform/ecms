<%
	String appId = ((String[])request.getAttribute("appId"))[0];
	String query = ((String[])request.getAttribute("query"))[0];
	String folder = ((String[])request.getAttribute("folder"))[0];
	String type = ((String[])request.getAttribute("type"))[0];
	String limit = System.getProperty("exo.dw.page.snapshot.itemsLimit", "10");
%>
<div class="VuetifyApp documents">
	<div data-app="true"
		class="v-application VuetifyApp v-application--is-ltr theme--light"
		id="<%=appId%>" app-id="<%=appId%>" flat="">
		<script>
			require(['SHARED/documentsBundle'],
				app => app.init("<%=appId%>", "<%=query%>", "<%=folder%>", "<%=type%>", "<%=limit%>")
			);
		</script>
		<div class="v-application--wrap">
			<div class="container pa-0">
				<div class="layout white row mx-0">
					<div class="flex xs12">
						<div class="layout row mx-0">
							<div role="list"
								class="v-list mx-3 pt-0 v-sheet v-sheet--tile theme--light v-list--two-line">
								<div tabindex="-1" role="listitem"
									class="pa-0 v-list-item theme--light">
									<div
										class="v-avatar v-list-item__avatar my-0 v-avatar--tile"
										href="#" style="height: 40px; min-width: 40px; width: 40px;">
										<div class="v-avatar v-avatar--tile"
											style="height: 37px; min-width: 37px; width: 37px;">
											<div
												class="v-responsive v-image mx-auto skeleton-background"
												style="height: 37px; max-height: 37px; max-width: 37px; width: 37px;">
												<div class="v-responsive__content"></div>
											</div>
										</div>
									</div>
									<div href="#" class="v-list-item__content pa-0">
										<div class="v-list-item__title">
											<div
												class="v-skeleton-loader mt-3 mr-3 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 11px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
										<div class="v-list-item__subtitle">
											<div
												class="v-skeleton-loader mb-2 mt-1 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 8px; width: 70px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
									</div>
								</div>
								<div tabindex="-1" role="listitem"
									class="pa-0 v-list-item theme--light">
									<div
										class="v-avatar v-list-item__avatar my-0 v-avatar--tile"
										href="#" style="height: 40px; min-width: 40px; width: 40px;">
										<div class="v-avatar v-avatar--tile"
											style="height: 37px; min-width: 37px; width: 37px;">
											<div
												class="v-responsive v-image mx-auto skeleton-background"
												style="height: 37px; max-height: 37px; max-width: 37px; width: 37px;">
												<div class="v-responsive__content"></div>
											</div>
										</div>
									</div>
									<div href="#" class="v-list-item__content pa-0">
										<div class="v-list-item__title">
											<div
												class="v-skeleton-loader mt-3 mr-3 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 11px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
										<div class="v-list-item__subtitle">
											<div
												class="v-skeleton-loader mb-2 mt-1 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 8px; width: 70px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
									</div>
								</div>
								<div tabindex="-1" role="listitem"
									class="pa-0 v-list-item theme--light">
									<div
										class="v-avatar v-list-item__avatar my-0 v-avatar--tile"
										href="#" style="height: 40px; min-width: 40px; width: 40px;">
										<div class="v-avatar v-avatar--tile"
											style="height: 37px; min-width: 37px; width: 37px;">
											<div
												class="v-responsive v-image mx-auto skeleton-background"
												style="height: 37px; max-height: 37px; max-width: 37px; width: 37px;">
												<div class="v-responsive__content"></div>
											</div>
										</div>
									</div>
									<div href="#" class="v-list-item__content pa-0">
										<div class="v-list-item__title">
											<div
												class="v-skeleton-loader mt-3 mr-3 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 11px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
										<div class="v-list-item__subtitle">
											<div
												class="v-skeleton-loader mb-2 mt-1 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 8px; width: 70px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
									</div>
								</div>
								<div tabindex="-1" role="listitem"
									class="pa-0 v-list-item theme--light">
									<div
										class="v-avatar v-list-item__avatar my-0 v-avatar--tile"
										href="#" style="height: 40px; min-width: 40px; width: 40px;">
										<div class="v-avatar v-avatar--tile"
											style="height: 37px; min-width: 37px; width: 37px;">
											<div
												class="v-responsive v-image mx-auto skeleton-background"
												style="height: 37px; max-height: 37px; max-width: 37px; width: 37px;">
												<div class="v-responsive__content"></div>
											</div>
										</div>
									</div>
									<div href="#"
										class="v-list-item__content pa-0">
										<div class="v-list-item__title">
											<div
												class="v-skeleton-loader mt-3 mr-3 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 11px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
										<div class="v-list-item__subtitle">
											<div
												class="v-skeleton-loader mb-2 mt-1 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 8px; width: 70px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
									</div>
								</div>
								<div tabindex="-1" role="listitem"
									class="pa-0 v-list-item theme--light">
									<div
										class="v-avatar v-list-item__avatar my-0 v-avatar--tile"
										href="#" style="height: 40px; min-width: 40px; width: 40px;">
										<div class="v-avatar v-avatar--tile"
											style="height: 37px; min-width: 37px; width: 37px;">
											<div
												class="v-responsive v-image mx-auto skeleton-background"
												style="height: 37px; max-height: 37px; max-width: 37px; width: 37px;">
												<div class="v-responsive__content"></div>
											</div>
										</div>
									</div>
									<div href="#"
										class="v-list-item__content pa-0">
										<div class="v-list-item__title">
											<div
												class="v-skeleton-loader mt-3 mr-3 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 11px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
										<div class="v-list-item__subtitle">
											<div
												class="v-skeleton-loader mb-2 mt-1 skeleton-background v-skeleton-loader--boilerplate v-skeleton-loader--is-loading theme--light"
												style="height: 8px; width: 70px;">
												<div class="v-skeleton-loader__text v-skeleton-loader__bone"></div>
											</div>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
			<!---->
		</div>
	</div>
</div>