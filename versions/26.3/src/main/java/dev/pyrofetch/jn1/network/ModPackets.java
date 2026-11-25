package dev.pyrofetch.jn1.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ModPackets {
	private ModPackets() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SpeakPayload.ID, SpeakPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ProgressSyncPayload.ID, ProgressSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTerminalPayload.ID, OpenTerminalPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenNotePayload.ID, OpenNotePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SaveNotePayload.ID, SaveNotePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HugPayload.ID, HugPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TerminalActionPayload.ID, TerminalActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ShopPurchasePayload.ID, ShopPurchasePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(JennyActionPayload.ID, JennyActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenCameraPayload.ID, OpenCameraPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PhotoDataPayload.ID, PhotoDataPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PhotoUploadPayload.ID, PhotoUploadPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PhotoRequestPayload.ID, PhotoRequestPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CameraAimPayload.ID, CameraAimPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenVendingPayload.ID, OpenVendingPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenPanelPayload.ID, OpenPanelPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenSelectorPayload.ID, OpenSelectorPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectorActionPayload.ID, SelectorActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LidarPayload.ID, LidarPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenScenesPayload.ID, OpenScenesPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ExcitationPayload.ID, ExcitationPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OrgasmePayload.ID, OrgasmePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenGenderPayload.ID, OpenGenderPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GenderChoicePayload.ID, GenderChoicePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PogoPayload.ID, PogoPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OnlyFapsPayload.ID, OnlyFapsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OnlyFapsEtatPayload.ID, OnlyFapsEtatPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ScenePayload.ID, ScenePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ScenePropPayload.ID, ScenePropPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SceneJoinPayload.ID, SceneJoinPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MorphSyncPayload.ID, MorphSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenMorphPayload.ID, OpenMorphPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MorphRequestPayload.ID, MorphRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MorphGesturePayload.ID, MorphGesturePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenNotebookPayload.ID, OpenNotebookPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenCompanionPayload.ID, OpenCompanionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TalkPayload.ID, TalkPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(GasterShopPayload.ID, GasterShopPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GasterShopActionPayload.ID, GasterShopActionPayload.CODEC);
	}
}
